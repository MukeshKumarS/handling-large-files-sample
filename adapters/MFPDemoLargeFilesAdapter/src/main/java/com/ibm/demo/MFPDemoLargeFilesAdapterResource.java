/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.ibm.demo;

import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;
import io.swagger.annotations.Api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Demo Large Files Adapter Resource")
@Path("/resource")
public class MFPDemoLargeFilesAdapterResource {
	/*
	 * For more info on JAX-RS see
	 * https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
	 */

    private final static String SAMPLE_LARGE_FILE_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
	private final static int BUFFER_SIZE = 8192;

	// Define logger (Standard java.util.Logger)
	static Logger logger = Logger.getLogger(MFPDemoLargeFilesAdapterResource.class.getName());

	// Inject the MFP configuration API:
	@Context
	ConfigurationAPI configApi;

	/**
	 * Using JAX-RS StreamingOutput to serve large files.
	 *
	 * @return
	 * @throws
	 */
	@GET
	@Path("/download")
	@OAuthSecurity(scope = "userScope")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public StreamingOutput getFileStreamingOutput()  {
		return new StreamingOutput() {
			public void write(OutputStream out) throws IOException {
				logger.info("Serving file: " + SAMPLE_LARGE_FILE_URL);
				InputStream in = new URL(SAMPLE_LARGE_FILE_URL).openStream();
				byte[] data = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = in.read(data)) != -1) {
					out.write(data, 0, read);
				}
				out.flush();
				out.close();
				in.close();
			}
		};
	}

    @POST
    @Consumes("application/octet-stream")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("uploader")
    @OAuthSecurity(scope = "userScope")
    public Response doUpload(@Context HttpServletRequest request, @QueryParam("fileName") String fileName) {
        logger.info("Serving upload file request: " + fileName);
        if(fileName == null || fileName.trim().equals(""))
            return Response.status(400).entity("Filename cannot be empty").build();
        try {
            this.saveToFile(request.getInputStream(), fileName);

            return Response
                    .status(200)
                    .entity(fileName + " file uploaded via adapter based RESTFul webservice.")
                    .build();

        } catch (Exception e) {
            String err = "Error serving upload file request.";
            logger.log(Level.SEVERE, err, e);
            return Response.status(500).entity(err + e.getMessage()).build();
        }
    }

	private void saveToFile(InputStream in,
							String filename) throws IOException {
		int read;
		byte[] data = new byte[BUFFER_SIZE];
		OutputStream out = new FileOutputStream(new File(System.getProperty("user.home")
				+ System.getProperty("file.separator") + filename));
		while ((read = in.read(data)) != -1) {
			out.write(data, 0, read);
		}
		out.flush();
		out.close();
		in.close();
	}

}
