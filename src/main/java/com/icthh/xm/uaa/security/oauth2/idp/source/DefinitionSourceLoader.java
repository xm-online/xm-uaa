package com.icthh.xm.uaa.security.oauth2.idp.source;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

//TODO java doc
//TODO Local storage - rejected
//TODO 2. Кеш на базе ms-config (подпись на файл). Свой endpoint  .well-know
public interface DefinitionSourceLoader {

    List<InputStream> retrieveRawPublicKeysDefinition(Map<String, Object> params);

}
