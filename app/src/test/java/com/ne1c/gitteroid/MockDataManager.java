package com.ne1c.gitteroid;

import com.ne1c.gitteroid.api.GitterApi;
import com.ne1c.gitteroid.dataproviders.ClientDatabase;
import com.ne1c.gitteroid.dataproviders.DataManger;

public class MockDataManager extends DataManger {
    public MockDataManager(GitterApi api, ClientDatabase database) {
        super(api, database);
    }
}
