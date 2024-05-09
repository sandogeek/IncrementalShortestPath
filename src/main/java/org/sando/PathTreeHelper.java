package org.sando;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 最短路径树辅助函数
 * @author Sando Geek
 * @since 2024/05/08
 * @version 1.0
 */
public class PathTreeHelper {
    private PathTreeHelper() {
    }

    public static <K, V extends BaseDijkVertex<K, V>> void handleSuccessorAndSelfRecursive(V vertexRoot,
                                                 Consumer<? super V> vertexConsumer) {
        vertexConsumer.accept(vertexRoot);
        handleSuccessorRecursive(vertexRoot, vertexConsumer, null);
    }

    public static <K, V extends BaseDijkVertex<K, V>> void handleSuccessorAndSelfRecursive(V vertexRoot,
                                                 Consumer<? super V> vertexConsumer, Function<? super V, Boolean> stopConsumeAndRecursive) {
        if (stopConsumeAndRecursive != null && stopConsumeAndRecursive.apply(vertexRoot)) {
            return;
        }
        vertexConsumer.accept(vertexRoot);
        handleSuccessorRecursive(vertexRoot, vertexConsumer, stopConsumeAndRecursive);
    }

    /**
     * 递归处理最短路径上的后继节点
     *
     * @param vertexRoot     起点
     * @param vertexConsumer 消费节点
     * @param stopRecursive  是否停止递归该节点的后继节点,并且该节点不会被消费
     */
    public static <K, V extends BaseDijkVertex<K, V>> void handleSuccessorRecursive(V vertexRoot,
                                          Consumer<? super V> vertexConsumer, Function<? super V, Boolean> stopRecursive) {
        vertexRoot.walkSuccessor(vertex -> {
            if (stopRecursive != null && stopRecursive.apply(vertex)) {
                return;
            }
            vertexConsumer.accept(vertex);
            handleSuccessorRecursive(vertex, vertexConsumer, stopRecursive);
        });
    }
}
