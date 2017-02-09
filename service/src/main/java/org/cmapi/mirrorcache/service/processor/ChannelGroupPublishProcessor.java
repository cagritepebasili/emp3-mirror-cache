package org.cmapi.mirrorcache.service.processor;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.cmapi.mirrorcache.Member;
import org.cmapi.mirrorcache.MirrorCacheException;
import org.cmapi.mirrorcache.MirrorCacheException.Reason;
import org.cmapi.mirrorcache.Priority;
import org.cmapi.mirrorcache.service.CacheManager;
import org.cmapi.mirrorcache.service.ChannelGroupManager;
import org.cmapi.mirrorcache.service.SessionManager;
import org.cmapi.mirrorcache.service.cache.CachedEntity;
import org.cmapi.mirrorcache.service.cache.EntityCache;
import org.cmapi.mirrorcache.support.ProtoMessageEntry;
import org.cmapi.mirrorcache.support.Utils;
import org.cmapi.primitives.proto.CmapiProto.ChannelGroupPublishCommand;
import org.cmapi.primitives.proto.CmapiProto.OneOfCommand;
import org.cmapi.primitives.proto.CmapiProto.ProtoMessage;
import org.cmapi.primitives.proto.CmapiProto.ProtoPayload;
import org.cmapi.primitives.proto.CmapiProto.Status;
import org.slf4j.Logger;

@ApplicationScoped
public class ChannelGroupPublishProcessor implements CommandProcessor {

    @Inject
    private Logger LOG;
    
    @Inject
    private ChannelGroupManager channelGroupManager;
    
    @Inject
    private CacheManager cacheManager;
    
//    @Inject
//    private HistoryManager historyManager;
    
    @Inject
    private SessionManager sessionManager;
    
    
    @Override
    public void process(String sessionId, ProtoMessage req) {
        
        if (req.hasPayload()) {
            final ChannelGroupPublishCommand command = req.getCommand().getChannelGroupPublish();
            
            /*
             * Update entity cache.
             */
            final EntityCache entityCache = cacheManager.getEntityCache();
            final CachedEntity entity     = entityCache.update(req.getPayload());
            
            /*
             * Update channel cache.
             */
            cacheManager.addToChannelGroupCache(sessionId, command.getChannelGroupName(), entity);
            
            /*
             * Update channel history.
             */
//            historyManager.logChannelGroupEntry(sessionId, command.getChannelGroupName(), req.getCommand());
            
            final ProtoMessage res = ProtoMessage.newBuilder(req)
                    .setPriority(Priority.LOW.getValue())
                    .setCommand(OneOfCommand.newBuilder()
                                            .setChannelGroupPublish(ChannelGroupPublishCommand.newBuilder(command)
                                                                                              .setStatus(Status.SUCCESS)))
                    .setPayload(ProtoPayload.newBuilder()
                                            .setId(req.getPayload().getId())
                                            .setType(req.getPayload().getType())
                                            .setData(req.getPayload().getData()))
                    .build();
            
            try {
                /*
                 * Distribute to participants of channelGroup.
                 */
                LOG.debug("distribute: " + Utils.asString(res));
                for (Member otherMember : channelGroupManager.channelGroupPublish(sessionId, command.getChannelGroupName())) {
                    LOG.debug("\t-> " + otherMember);

                    if (!sessionManager.getOutboundQueue(otherMember.getSessionId()).offer(new ProtoMessageEntry(res), 1, TimeUnit.SECONDS)) {
                        throw new RuntimeException(Reason.QUEUE_OFFER_TIMEOUT.getMsg() + ", sessionId: " + otherMember.getSessionId());
                    }
                }

            } catch (MirrorCacheException e) {
                throw new RuntimeException("ERROR: " + e.getMessage(), e);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn(Thread.currentThread().getName() + " thread was interrupted.");
            }
        }
    }
}
