package com.icthh.xm.uaa.security.oauth2.idp.source;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

//TODO java doc
public interface DefinitionSourceLoader {

    List<InputStream> retrieveRawPublicKeysDefinition(Map<String, Object> params);

}
