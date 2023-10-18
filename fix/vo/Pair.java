package jimmy.practice.spd.component.storage.fix.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair {
    private ApproveLogVO actual;
    private ApproveLogVO expected;
}
