import android.widget.Button;

import com.ne1c.developerstalk.Activities.LoginActivity;
import com.ne1c.developerstalk.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {
    @Test
    public void clickingButton() throws Exception {
        LoginActivity activity = Robolectric.setupActivity(LoginActivity.class);

        Button b = (Button) activity.findViewById(R.id.auth_but);
        b.performClick();
    }
}
