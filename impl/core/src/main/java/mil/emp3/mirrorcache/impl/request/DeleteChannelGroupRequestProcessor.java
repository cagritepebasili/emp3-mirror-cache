package mil.emp3.mirrorcache.impl.request;

import org.cmapi.primitives.proto.CmapiProto.DeleteChannelGroupOperation;
import org.cmapi.primitives.proto.CmapiProto.OneOfOperation;
import org.cmapi.primitives.proto.CmapiProto.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.emp3.mirrorcache.Message;
import mil.emp3.mirrorcache.MessageDispatcher;
import mil.emp3.mirrorcache.MirrorCacheException;
import mil.emp3.mirrorcache.MirrorCacheException.Reason;
import mil.emp3.mirrorcache.Priority;

public class DeleteChannelGroupRequestProcessor extends BaseRequestProcessor<Message, Void> {
    static final private Logger LOG = LoggerFactory.getLogger(DeleteChannelGroupRequestProcessor.class);
    
    final private MessageDispatcher dispatcher;
    
    public DeleteChannelGroupRequestProcessor(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Void executeSync(Message reqMessage) throws MirrorCacheException {
        if (reqMessage == null) {
            throw new IllegalStateException("reqMessage == null");
        }
        
        dispatcher.dispatchMessage(reqMessage.setPriority(Priority.MEDIUM));
        
        try {
            final Message resMessage = dispatcher.awaitResponse(reqMessage);
            
            final DeleteChannelGroupOperation operation = resMessage.getOperation().as(OneOfOperation.class).getDeleteChannelGroup();
            if (operation.getStatus() == Status.SUCCESS) {
                return null;
                
            } else {
                throw new MirrorCacheException(Reason.DELETE_CHANNELGROUP_FAILURE).withDetail("channelGroupName: " + operation.getChannelGroupName());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info(Thread.currentThread().getName() + " thread interrupted.");
        }
        
        return null;
    }
}