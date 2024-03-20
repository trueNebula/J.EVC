package jevc;

import jevc.entities.RGBImage;

import java.io.IOException;

public class JPEGDecoder {

    public static void main(String[] args) throws IOException {
        if (args.length<1) {
            System.err.println("Run the program: java pdav.JPEGDecoder imageFile!");
        }

        if (!args[0].substring(args[0].indexOf("."), args[0].length()).equals(".jpg")) {
            System.err.println("The first parameter should be a .jpg image");
        }
        String outfile = args[0].substring(0, args[0].indexOf(".")) + ".png";

        Decoder decoder = new Decoder(args[0]);
        decoder.readBitStream();
        RGBImage decodedImage = decoder.decode();
        decodedImage.writePNGFile(outfile);
    }
}
