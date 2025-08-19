package jimmy.practice.spd.component.user;

import jimmy.practice.basic.common.concurrent.Tasks;
import jimmy.practice.basic.common.spring.SpringUtil;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static org.springframework.transaction.TransactionDefinition.*;

/**
 * 编程式事务管理的工具类
 *
 * @author jimmy
 */
public final class TransactionUtil {
    private static final ConcurrentMap<Integer, TransactionTemplate> transactionTemplateMap = new ConcurrentHashMap<>(4);

    private TransactionUtil() {
    }

    public static void required(Runnable logic) {
        required(Tasks.toSupplier(logic));
    }

    public static <T> T required(Supplier<T> logic) {
        return getTransactionTemplate(PROPAGATION_REQUIRED).execute(ts -> logic.get());
    }

    public static void requiredNew(Runnable logic) {
        requiredNew(Tasks.toSupplier(logic));
    }

    public static <T> T requiredNew(Supplier<T> logic) {
        return getTransactionTemplate(PROPAGATION_REQUIRES_NEW).execute(ts -> logic.get());
    }

    public static void nested(Runnable logic) {
        nested(Tasks.toSupplier(logic));
    }

    public static <T> T nested(Supplier<T> logic) {
        return getTransactionTemplate(PROPAGATION_NESTED).execute(ts -> logic.get());
    }

    public static void execute(DefaultTransactionDefinition transactionDefinition, Runnable logic) {
        execute(transactionDefinition, Tasks.toSupplier(logic));
    }

    public static <T> T execute(DefaultTransactionDefinition transactionDefinition, Supplier<T> logic) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(SpringUtil.getBean(PlatformTransactionManager.class), transactionDefinition);
        return transactionTemplate.execute(ts -> logic.get());
    }

    private static TransactionTemplate getTransactionTemplate(int propagationBehavior) {
        return transactionTemplateMap.computeIfAbsent(propagationBehavior, key -> {
            DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
            transactionDefinition.setPropagationBehavior(propagationBehavior);
            return new TransactionTemplate(SpringUtil.getBean(PlatformTransactionManager.class), transactionDefinition);
        });
    }

}




