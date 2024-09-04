package jimmy.practice.spd.component.user.poi.vo;

import lombok.Data;

import java.util.List;

@Data
public class VirtualUserVO {
    private String user;
    private List<String> roles;
}
