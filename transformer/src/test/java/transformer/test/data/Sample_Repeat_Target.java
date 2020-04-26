package transformer.test.data;

import transformer.test.data.javax.repeat.Sample_Repeated;
import transformer.test.data.javax.repeat.Sample_Repeats;

@Sample_Repeated(value = 1, name = "one")
@Sample_Repeated(value = 2, name = "two")
public class Sample_Repeat_Target {
	@Sample_Repeats(
		value = {
			@Sample_Repeated(value = 3, name = "three" ),
			@Sample_Repeated(value = 4, name = "four")
		})
	public int testMethod() {
		return 1;
	}
}
