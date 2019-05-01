package csci576;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

public class LogoDetection
{

    public static String detectLogos(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<String> logos = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            String foundLogo = null;
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return null;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
                    foundLogo = annotation.getDescription();
                }

            }
            return foundLogo;
        }
    }

    public static List<String> matchLogoToImage( ByteString byteString) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<String> matched = new ArrayList<>();

        
        //ByteString imgBytes = ByteString.readFrom(in);
        //ByteString imgBytes = ByteString.readFrom(img);

        try{

            //the next few lines create a byte array from the BufferedImage
            //you can comment these out and have this function take in a byte array directly
            //not sure if this works with a BufferedImage - I ran it on the video and it
            //didn't look like it was detecting anything but i didn't get an error back from the API
            
            /*
            ByteArrayOutputStream baos = new ByteArrayOutputStream(imgBytes);
            ImageIO.write( img, "jpg", baos );
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            .close();
            int width = 480;
            int height = 270;
            BufferedImage bufferedImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            IntBuffer intBuf = ByteBuffer.wrap(imgBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] array = new int[intBuf.remaining()];
            intBuf.get(array);
            bufferedImg.setRGB(0, 0, width, height, array, 0, width);
            
            ImageIO.write(bufferedImg, "jpg", new File("frame.jpg"));
            ByteString bytes = ByteString.readFrom(new FileInputStream("frame.jpg"));
            Image img = Image.newBuilder().setContent(imgBytes).build();
*/
            //below is where you would input the byte array (this turns the array into ByteString
            //which is needed for this code to work
            //ByteString imgBytes = ByteString.copyFrom(imageInByte);
            Image img2 = Image.newBuilder().setContent(byteString).build();
            try{
                //Image img1 = Image.parseFrom(byteString);
                
                Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
                AnnotateImageRequest request =
                        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img2).build();
                requests.add(request);
    
                try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                    BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                    List<AnnotateImageResponse> responses = response.getResponsesList();
    
                    for (AnnotateImageResponse res : responses) {
                        if (res.hasError()) {
                            //System.out.println("Error: "+ res.getError().getMessage());
                            return new ArrayList<>();
                        }
    
                        // For full list of available annotations, see http://g.co/cloud/vision/docs
                        for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
                            String description = annotation.getDescription();
                            System.out.println("img detected... " + annotation.getDescription());
                            matched.add(description);
                        }
    
                    }
                    return matched;
                }
    
            }catch(IOException e){
    
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return null;
    }



    public static void getNewAds(File dir, List<String> matched) {
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                for( String match : matched) {
                    if(child.getName().contains(match)) {
                        System.out.println("Found The Ad File To Insert: " + child.getName());
                    }
                }
                System.out.println("file: " + child.getName());
            }
        } else {
            System.out.println("No Brand Images Found");
        }
    }

    public static List<String> run()
    {
        //you'll obviously need to change this - this is just where I put the given dataset folder
        String path = "/Users/mukesh/Downloads/dataset/";
        try {
            List<String> logos = new ArrayList<>();
            File dir = new File(path + "Brand Images");
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if(child.getName().contains(".bmp") || child.getName().contains(".jpg")) {
                        logos.add(detectLogos(path + "Brand Images/" + child.getName(), System.out));
                    }
                }

    		    /*call for each frame?
    		    List<String> matches = matchLogoToImage(framePath + "starbucks2.jpg", System.out, logos, null);
    		    for(int j = 0; j < matches.size(); j++) {
    		    	System.out.println("MATCH FOUND: " + matches.get(j));
    		    }*/

    		    /*find ad to replace
    		    File adDir = new File(path + "Ads");
    		    getNewAds(adDir, matches);

    		    */
                return logos;
            } else {
                System.out.println("No Brand Images Found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
