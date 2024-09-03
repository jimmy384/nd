package jimmy.practice.spd.component.user.cookie.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * Cookie信息
 *
 * @author jimmy
 */
@Data
public class Cookie {
    @JSONField(name = "Host")
    private String host;
    @JSONField(name = "Path")
    private String path;
    @JSONField(name = "KeyName")
    private String keyName;
    @JSONField(name = "Value")
    private String value;
    @JSONField(name = "IsSecure")
    private String isSecure;
    @JSONField(name = "IsHTTPOnly")
    private String isHTTPOnly;
    @JSONField(name = "HasExpire")
    private String hasExpire;
    @JSONField(name = "IsPersistent")
    private String isPersistent;
    @JSONField(name = "CreateDate")
    private String createDate;
    @JSONField(name = "ExpireDate")
    private String expireDate;

    public String getKeyValue() {
        return keyName + "=" + value;
    }
}
