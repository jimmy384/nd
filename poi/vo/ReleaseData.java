package jimmy.practice.spd.component.user.poi.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReleaseData {
    private String pi;
    private List<UserStoryVO> usList;
    private boolean hasLookup;
    private boolean hasI18n;
    private boolean hasDict;
    private List<StarlingParameterVO> starlingParameterVOs;
    private List<ConfigCenterVO> configCenterVOS;
    private List<PublishApiVO> publishApiVOS;
    private List<SubscribeApiVO> subscribeApiVOs;
    private List<VirtualUserVO> virtualUserVOS;
    private List<DeployUnitVO> deployUnitVOS;
    private List<DbScriptVO> dbScriptVOS;
}
