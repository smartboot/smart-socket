/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: CheckFilterGroup.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class CheckFilterGroup {

    private static CheckFilterGroup group;
    private CheckFilter checkFilter;

    private CheckFilterGroup() {
        checkFilter = new MethodCheckFilter();
        checkFilter.next(new URICheckFilter());
    }

    public static final CheckFilterGroup group() {
        if (group != null) {
            return group;
        }
        synchronized (CheckFilterGroup.class) {
            if (group != null) {
                return group;
            }
            group = new CheckFilterGroup();
        }
        return group;
    }

    public CheckFilter getCheckFilter() {
        return checkFilter;
    }
}
