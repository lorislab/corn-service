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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.lorislab.corn.Corn;
import org.lorislab.corn.CornExecutor;
import org.lorislab.corn.CornRequest;
import org.lorislab.corn.file.FileObject;
import org.lorislab.corn.zip.ZipObject;

/**
 * The corn service.
 *
 * @author andrej
 */
@Path("service")
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CornRestService {

    /**
     * The logger for this class.
     */
    private final static Logger LOG = Logger.getLogger(CornRestService.class.getName());

    /**
     * The target directory system environment property.
     */
    public final static String PROP_TARGET_DIR = "CORN_TARGET";

    /**
     * The clean up system environment property.
     */
    public final static String PROP_CLEAN_UP = "CORN_CLEANUP";

    /**
     * The target parent directory.
     */
    public final static String TARGET;

    /**
     * The clean up flag.
     */
    public final static boolean CLEANUP;

    /**
     * The static block.
     */
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

    /**
     * Creates the zip package as ouput from the CORN service.
     *
     * @param request the request.
     * @return the corresponding data.
     * @throws Exception if the method fails.
     */
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
            result = createZipData(target);
            if (CLEANUP) {
                FileObject.delete(path);
            }
        }
        return result;
    }
    
    @GET
    @Path("data/{data}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object config(@PathParam("data") String data) throws Exception {
        Object result = null;
        java.nio.file.Path path = Paths.get(data);
        if (Files.exists(path)) {
            result =  Corn.loadJson(data);
        } else {
            LOG.log(Level.WARNING, "The data file {0} does not exists", data);            
        }
        return result;
    }
    
    /**
     * Creates the ZIP data.
     *
     * @param dirName the directory.
     * @return the corresponding data.
     */
    public static byte[] createZipData(String dirName) {
        try {
            java.nio.file.Path result = Files.createTempFile("corn", ".zip");
            java.nio.file.Path dir = Paths.get(dirName);
            ZipObject.createDiretoryZip(result, dir, dir);
            return Files.readAllBytes(result);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }    
}
