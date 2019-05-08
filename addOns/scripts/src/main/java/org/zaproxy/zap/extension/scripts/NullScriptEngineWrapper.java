/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2017 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.scripts;

import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import org.zaproxy.zap.extension.script.DefaultEngineWrapper;

public class NullScriptEngineWrapper extends DefaultEngineWrapper {

    public static final String NAME = "Null";

    public NullScriptEngineWrapper(ScriptEngine engine) {
        super(engine);
    }

    @Override
    public String getLanguageName() {
        return "";
    }

    @Override
    public String getEngineName() {
        return NAME;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.emptyList();
    }

    // TODO Uncomment the annotation once targeting newer core version.
    // @Override
    public boolean isVisible() {
        return false;
    }
}
