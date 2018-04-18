package downloadbasket.helpers;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.IOHelper;
import downloadbasket.data.ErrorReportDetails;
import downloadbasket.data.LoadZipDetails;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.JTS;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.mail.MultiPartEmail;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import com.opencsv.CSVReader;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.geotools.data.DataStore;
import org.geotools.data.ogr.OGRDataStore;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.data.ogr.bridj.BridjOGRDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.context.MessageSource;
import fi.nls.oskari.spring.SpringContextHolder;

/**
 * Download services.
 */
public class DownloadServices {
    private final Logger LOGGER = LogFactory.getLogger(DownloadServices.class);
    private static final char CSV_FILE_DELIMITER = ',';
    private MessageSource messages;

    /**
     * Default Constructor.
     */
    public DownloadServices() {
    }

    /**
     * Load shape-ZIP from Geoserver.
     *
     * @param ldz load zip details
     * @return filename file name
     * @throws IOException Normal way download uses BBOX as the cropping method.
     *                     Otherwise, filter plugin is used.
     */
    public String loadZip(LoadZipDetails ldz, Locale locale) throws IOException {
        String zipFileName = null;
        HttpURLConnection conn = null;
        CoordinateReferenceSystem sourceCrs;
        CoordinateReferenceSystem targetCrs;
        MathTransform transform = null;
        SimpleFeatureIterator it;
        SimpleFeature feature;

        try {
            LOGGER.debug("WFS URL: " + ldz.getWFSUrl());

            if (ldz.isDownloadNormalWay()) {
                LOGGER.debug("Download normal way");
            }

            LOGGER.debug("WFS URL: " + ldz.getWFSUrl());
            LOGGER.debug("-- filter: " + ldz.getGetFeatureInfoRequest());
            final URL url = new URL(ldz.getWFSUrl() + ldz.getGetFeatureInfoRequest());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(600000);

            conn.connect();

            String basketId = UUID.randomUUID().toString();
            String strTempDir = ldz.getTemporaryDirectory();
            String strOutputDir = strTempDir + File.separator + basketId;

            File dir0 = new File(strTempDir);
            File outputDir = new File(strOutputDir);
            outputDir.mkdirs();

            String gmlFileName = basketId + ".gml";
            File gmlFile = new File(dir0, gmlFileName);

            try (InputStream istream = conn.getInputStream();
                 OutputStream ostream = new FileOutputStream(gmlFile)) {
                IOHelper.copy(istream, ostream);

                Map<String, String> connectionParams = new HashMap<>();
                connectionParams.put("DriverName", "GML");
                connectionParams.put("DatasourceName", new File(dir0, gmlFileName).getAbsolutePath());
                OGRDataStoreFactory factory = new BridjOGRDataStoreFactory();
                if(!factory.isAvailable()){
                    LOGGER.error("GDAL library is not found for data export -- http://www.gdal.org/");
                    return null;
                }
                DataStore store = factory.createDataStore(connectionParams);
                String[] typeNames = store.getTypeNames();

                sourceCrs = CRS.decode("EPSG:3067",true);
                targetCrs = CRS.decode("EPSG:4326", true);
                if (!targetCrs.getName().equals(sourceCrs.getName())) {
                    transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
                }

                for (int i = 0; i < typeNames.length; i++) {
                    SimpleFeatureSource source = store.getFeatureSource(typeNames[i]);
                    SimpleFeatureCollection features3067 = source.getFeatures();
                    DefaultFeatureCollection features4326 = new DefaultFeatureCollection();

                    // Coordinate transformation
                    it = features3067.features();
                    while (it.hasNext()) {
                        feature = it.next();
                        if (transform != null) {
                            Geometry geometry = (Geometry) feature.getDefaultGeometry();
                            feature.setDefaultGeometry(JTS.transform(geometry, transform));
                            features4326.add(feature);
                        }
                    }
                    it.close();

                    // CSV
                    String csvFileName = typeNames[i] + basketId + ".csv";
                    File csvFile = new File(dir0, csvFileName);
                    Map<String, String> connectionParamsCsv = new HashMap<>();
                    connectionParamsCsv.put("DriverName", "CSV");
                    String csvFilePath = csvFile.getAbsolutePath();
                    connectionParamsCsv.put("DatasourceName", csvFilePath);
                    OGRDataStoreFactory factoryCsv = new BridjOGRDataStoreFactory();
                    OGRDataStore dataStoreCsv = (OGRDataStore) factoryCsv.createNewDataStore(connectionParamsCsv);
                    dataStoreCsv.createSchema(features4326, true, new String[]{
                        "GEOMETRY=AS_YX"
                    });

                    // Excel
                    File xlsFile = new File(outputDir, typeNames[i] + basketId + ".xls");
                    convertCsvToXls(xlsFile.getAbsolutePath(), csvFilePath);

                    // GPX
                    String gpxFileName = typeNames[i] + basketId + ".gpx";
                    File gpxFile = new File(outputDir, gpxFileName);
                    Map<String, String> connectionParamsGpx = new HashMap<>();
                    connectionParamsGpx.put("DriverName", "GPX");
                    connectionParamsGpx.put("DatasourceName", gpxFile.getAbsolutePath());
                    OGRDataStoreFactory factoryGpx = new BridjOGRDataStoreFactory();
                    OGRDataStore dataStoreGpx = (OGRDataStore) factoryGpx.createNewDataStore(connectionParamsGpx);
                    dataStoreGpx.createSchema(features4326, true, new String[]{
                        "GPX_USE_EXTENSIONS=YES"
                    });

                    // Shapefile
                    String shpFileName = typeNames[i] + basketId;
                    File shpFile = new File(outputDir, shpFileName);
                    Map<String, String> connectionParamsShp = new HashMap<>();
                    connectionParamsShp.put("DriverName", "ESRI Shapefile");
                    connectionParamsShp.put("DatasourceName", shpFile.getAbsolutePath());
                    OGRDataStoreFactory factoryShp = new BridjOGRDataStoreFactory();
                    OGRDataStore dataStoreShp = (OGRDataStore) factoryShp.createNewDataStore(connectionParamsShp);
                    dataStoreShp.createSchema(features3067, true, null);
                }
                File zipFile = new File(dir0, basketId + ".zip");
                zipFileName = zipFile.getAbsolutePath();
                pack(outputDir.getAbsolutePath(), zipFileName);
            } catch (IOException ioe) {
                LOGGER.error("Error: ", ioe);
            }
        } catch (Exception ex) {
            LOGGER.error("Error: ", ex);
        }
        return zipFileName;
    }

    public void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException ex) {
                        LOGGER.error("Error: ", ex);
                    }
                });
        }
    }

    public String convertCsvToXls(String xlsFilePath, String csvFilePath) {
        HSSFSheet sheet = null;
        CSVReader reader = null;
        HSSFWorkbook workBook = null;
        String generatedXlsFilePath = xlsFilePath;
        FileOutputStream fileOutputStream = null;

        try {
            /**** Get the CSVReader Instance & Specify The Delimiter To Be Used ****/
            String[] nextLine;
            reader = new CSVReader(new FileReader(csvFilePath), CSV_FILE_DELIMITER);
            workBook = new HSSFWorkbook();
            sheet = (HSSFSheet) workBook.createSheet("Sheet");
            int rowNum = 0;
            LOGGER.info("Creating New .Xls File From The Already Generated .Csv File");
            while((nextLine = reader.readNext()) != null) {
                Row currentRow = sheet.createRow(rowNum++);
                for(int i=0; i < nextLine.length; i++) {
                    if(NumberUtils.isDigits(nextLine[i])) {
                        currentRow.createCell(i).setCellValue(Integer.parseInt(nextLine[i]));
                    } else if (NumberUtils.isNumber(nextLine[i])) {
                        currentRow.createCell(i).setCellValue(Double.parseDouble(nextLine[i]));
                    } else {
                        currentRow.createCell(i).setCellValue(nextLine[i]);
                    }
                }
            }

            LOGGER.info("The File Is Generated At The Following Location?= " + generatedXlsFilePath);

            fileOutputStream = new FileOutputStream(generatedXlsFilePath);
            workBook.write(fileOutputStream);
        } catch(Exception exObj) {
            LOGGER.error("Exception In convertCsvToXls() Method?=  " + exObj);
        } finally {
            try {

                /**** Closing The Excel Workbook Object ****/
                workBook.close();

                /**** Closing The File-Writer Object ****/
                fileOutputStream.close();

                /**** Closing The CSV File-ReaderObject ****/
                reader.close();
            } catch (IOException ioExObj) {
                LOGGER.error("Exception While Closing I/O Objects In convertCsvToXls() Method?=  " + ioExObj);
            }
        }
        return generatedXlsFilePath;
    }

    /**
     * Check if zipfile is valid.
     *
     * @param file zip file
     * @return
     */
    public boolean isValid(File file) {

        try (ZipFile zipFile = new ZipFile(file)) {
            return true;
        } catch (IOException e) {
            LOGGER.debug("Zip-file is not valid", e);
            return false;
        }
    }

    private MessageSource getMessages() {
        if (messages == null) {
            // "manual autowire"
            messages = SpringContextHolder.getBean(MessageSource.class);

        }
        return messages;
    }

    private String getMessage(String key, String language) {
        return getMessages().getMessage(key, new String[]{}, new Locale(language));
    }

    public void sendErrorReportToEmail(ErrorReportDetails errorDetails) {
        try {
            String msg = getMessage("oskari.wfs.error.message", errorDetails.getLanguage());

            // Using Multipart because HtmlEmail doesn't handle attachments very
            // well.
            MultiPartEmail email = new MultiPartEmail();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(msg, "text/html; charset=UTF-8");
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            byte[] bytes = errorDetails.getXmlRequest().getBytes();

            DataSource dataSource = new ByteArrayDataSource(bytes, "application/xml");
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setDataHandler(new DataHandler(dataSource));
            bodyPart.setFileName("wfs_request.xml");
            multipart.addBodyPart(bodyPart);

            if (errorDetails.getErrorFileLocation() != null) {
                DataSource source = new FileDataSource(errorDetails.getErrorFileLocation());
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(new DataHandler(source));
                part.setFileName("geoserver_wfs_response.xml");
                multipart.addBodyPart(part);
            }

            email.setSmtpPort(Integer.parseInt(PropertyUtil.getNecessary(("oskari.wfs.download.smtp.port"))));
            email.setCharset("UTF-8");

            email.setContent(multipart);
            email.setHostName(PropertyUtil.getNecessary("oskari.wfs.download.smtp.host"));
            email.setFrom(PropertyUtil.getNecessary("oskari.wfs.download.email.from"));
            email.setSubject(getMessage("oskari.wfs.download.error.report.subject", errorDetails.getLanguage()));
            email.addTo(errorDetails.getUserEmail());
            email.addBcc(PropertyUtil.getNecessary("oskari.wfs.download.error.report.support.email"));
            email.send();
        } catch (Exception ex) {
            LOGGER.error(ex, "Error: e-mail was not sent");
        }
    }
}
