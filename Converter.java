package jimmy.practice.spd.component.user;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * 对象转换器
 * 正向转换：从S转换成T
 * 反向转换：从T转换成S
 *
 * @param <S>
 * @param <T>
 */
public interface Converter<S, T> {
    /**
     * 正向转换
     */
    T sourceToTarget(S source);

    /**
     * 反向转换
     */
    S targetToSource(T target);

    /**
     * 正向转换
     */
    default List<T> sourceToTarget(List<S> source) {
        return source.stream().map(this::sourceToTarget).toList();
    }

    /**
     * 反向转换
     */
    default List<S> targetToSource(List<T> target) {
        return target.stream().map(this::targetToSource).toList();
    }

    /**
     * 进行正向转换，后续可追加其他Converter继续转换
     * 后续使用then(converter), 后续的converter进行正向转换
     * 后续使用thenReverse(converter), 后续的converter进行反向转换
     */
    default <V> Middle<V> transform(S source, Function<T, V> logic) {
        T target = this.sourceToTarget(source);
        V logicValue = logic.apply(target);
        return new Middle<>(logicValue);
    }

    /**
     * 进行反向转换，后续可追加其他Converter继续转换
     * <p>
     * 后续使用thenReverse(converter), 后续的converter进行反向转换
     */
    default <V> Middle<V> reverse(T target, Function<S, V> logic) {
        S source = this.targetToSource(target);
        V logicValue = logic.apply(source);
        return new Middle<>(logicValue);
    }

    /**
     * 进行正向转换，后续可追加其他Converter继续转换
     * 后续使用then(converter), 后续的converter进行正向转换
     * 后续使用thenReverse(converter), 后续的converter进行反向转换
     */
    default <V> Middle<V> transform(List<S> source, Function<List<T>, V> logic) {
        List<T> target = this.sourceToTarget(source);
        V logicValue = logic.apply(target);
        return new Middle<>(logicValue);
    }

    /**
     * 进行反向转换，后续可追加其他Converter继续转换
     * 后续使用then(converter), 后续的converter进行正向转换
     * 后续使用thenReverse(converter), 后续的converter进行反向转换
     */
    default <V> Middle<V> reverse(List<T> target, Function<List<S>, V> logic) {
        List<S> source = this.targetToSource(target);
        V logicValue = logic.apply(source);
        return new Middle<>(logicValue);
    }

    @RequiredArgsConstructor
    class Middle<S> {
        final S value;

        /**
         * then(converter), 后续的converter进行正向转换
         * 可继续链式调用
         */
        public <T> Middle<T> then(Converter<S, T> converter) {
            T newValue = converter.sourceToTarget(this.value);
            return new Middle<>(newValue);
        }

        /**
         * thenGet(converter), 后续的converter进行正向转换
         * converter转换后返回结果，结束链式调用
         */
        public <T> T thenGet(Converter<S, T> converter) {
            return converter.sourceToTarget(this.value);
        }

        /**
         * thenReverse(converter), 后续的converter进行反向转换
         * 可继续链式调用
         */
        public <T> Middle<T> thenReverse(Converter<T, S> converter) {
            T newValue = converter.targetToSource(this.value);
            return new Middle<>(newValue);
        }

        /**
         * thenReverseGet(converter), 后续的converter进行反向转换
         * converter转换后返回结果，结束链式调用
         */
        public <T> T thenReverseGet(Converter<T, S> converter) {
            return converter.targetToSource(this.value);
        }

        /**
         * 返回结果，结束链式调用
         */
        public S get() {
            return value;
        }
    }

}
