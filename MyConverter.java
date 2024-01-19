package jimmy.practice.spd.component.user;

import jimmy.practice.spd.api.component.user.vo.response.UserVO;
import jimmy.practice.spd.component.user.mybatis.entity.UcUser;

import java.util.List;
import java.util.function.Function;

public class MyConverter<S, T> {
    public T convert(S source) {
        return null;
    }

    public List<T> convert(List<S> source) {
        return null;
    }

    public <V> V transform(S source, Function<T, V> logic) {
        T target = this.convert(source);
        return logic.apply(target);
    }

    public <V> V transform(List<S> source, Function<List<T>, V> logic) {
        List<T> target = this.convert(source);
        return logic.apply(target);
    }

    public static void main(String[] args) {
        MyConverter<UcUser, UserVO> myConverter = new MyConverter<>();
        UcUser ucUser = null;
        System.out.println("hello world");
        UserVO newUserVo = myConverter.transform(ucUser, vo -> {
            return new UserVO();
        });

        UserVO newUserVo2 = myConverter.transform(List.of(new UcUser()), vo -> {
            return new UserVO();
        });
    }

}
