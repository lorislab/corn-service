/*
 * Copyright 2018 lorislab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lorislab.corn.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.lorislab.corn.CornExecutor;
import org.lorislab.corn.CornRequest;

/**
 * The corn service.
 * 
 * @author andrej
 */
@Path("service")
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CornRestService {

    private final static Logger LOG = Logger.getLogger(CornRestService.class.getName());

    public final static String PROP_TARGET_DIR = "CORN_TARGET";

    public final static String PROP_CLEAN_UP = "CORN_CLEANUP";
    
    public final static String TARGET;
    
    public final static boolean CLEANUP;

    static {
        String target = System.getenv(PROP_TARGET_DIR);
        if (target == null || target.isEmpty()) {
            target = System.getProperty(PROP_TARGET_DIR, "target/");
        }
        TARGET = target;
        
        String c = System.getenv(PROP_TARGET_DIR);
        if (c == null || c.isEmpty()) {
            c = System.getProperty(PROP_CLEAN_UP, "true");
        }
        CLEANUP = Boolean.parseBoolean(c);
    }

    @POST
    @Path("zip")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/zip")
    public byte[] createZip(CornRequest request) throws Exception {

        String target = TARGET + UUID.randomUUID().toString();
        try {
            CornExecutor.execute(request, target);
        } catch (ScriptException ex) {
            LOG.log(Level.SEVERE, "Script file: {0}, Column : {1}, Line : {2}, Message : {3}", new Object[]{ex.getFileName(), ex.getColumnNumber(), ex.getLineNumber(), ex.getMessage()});
            throw new RuntimeException("Error: " + ex.getMessage(), ex);
        }

        byte[] result = null;
        java.nio.file.Path path = Paths.get(target);
        if (Files.exists(path)) {
            result = CornServiceUtil.createZipData(target);
            if (CLEANUP) {
                CornServiceUtil.deleteDirectory(path);
            }
        }
        return result;
    }
}
