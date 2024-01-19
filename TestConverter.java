package jimmy.practice.spd.component.user;

import jimmy.practice.spd.api.component.user.vo.response.UserSensitiveVO;
import jimmy.practice.spd.api.component.user.vo.response.UserVO;
import jimmy.practice.spd.component.user.mybatis.entity.UcUser;

public class TestConverter {
    private static class Converter1 implements Converter<UcUser, UserVO> {
        @Override
        public UserVO sourceToTarget(UcUser source) {
            UserVO userVO = new UserVO();
            userVO.setId(source.getId());
            return userVO;
        }

        @Override
        public UcUser targetToSource(UserVO target) {
            UcUser ucUser = new UcUser();
            ucUser.setId(target.getId());
            return ucUser;
        }
    }

    private static class Converter2 implements Converter<UserVO, UserSensitiveVO> {
        @Override
        public UserSensitiveVO sourceToTarget(UserVO source) {
            UserSensitiveVO userSensitiveVO = new UserSensitiveVO();
            userSensitiveVO.setIdCard(String.valueOf(source.getId()));
            return userSensitiveVO;
        }

        @Override
        public UserVO targetToSource(UserSensitiveVO target) {
            UserVO userVO = new UserVO();
            userVO.setId(Long.parseLong(target.getIdCard()));
            return userVO;
        }
    }


    public static void main(String[] args) {
        Converter1 converter1 = new Converter1();
        Converter2 converter2 = new Converter2();

        UcUser ucUser = new UcUser();
        ucUser.setId(123L);
        UserSensitiveVO newUserVo = converter1.transform(ucUser, vo -> {
            UserVO userVO = new UserVO();
            userVO.setId(vo.getId());
            return userVO;
        }).thenGet(converter2);
        System.out.println(newUserVo);

        UserSensitiveVO userSensitiveVO = new UserSensitiveVO();
        userSensitiveVO.setIdCard("321");
        UcUser ucUser1 = converter2.reverse(userSensitiveVO, vo -> {
            UserVO userVO = new UserVO();
            userVO.setId(vo.getId());
            return userVO;
        }).thenReverseGet(converter1);
        System.out.println(ucUser1);

    }
}
