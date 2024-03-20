package jevc.utils;


/**
 * File JPGInfoDump.java
 * JPGInfoDump class dumps on the screen the header+compressed data of a JPEG/JFIF image file
 *
 * @author forest (forest@ubbcluj.ro)
 * @version 1.01
 */

/**
 * THE FORMAT OF A JPEG/JFIF FILE IS:
 * -Header:
 * 	.It occupies two bytes.
 * 	.0xff, 0xd8 (SOI : Start Of Image ) (these two identify a JPEG/JFIF file).
 * -Segments or markers:
 * 	.Following the SOI marker, there can be any number of "segments" or "markers" such as:
 *	 APP0..APP15, SOF0..SOF15, DQT, DHT, SOS, JPG, JPG0..JPG13, DAC, DNL, DRI, DHP, EXP, RST0..RST7, TEM, COM.
 *      .An APP0 segment immediately follows the SOI marker.
 * -Trailer:
 * 	.It occupies two bytes.
 *      .0xff, 0xd9 (EOI: End of Image) (these two identify end of image).
 *
 * Note: any number of 0xff bytes between two segments (markers) must be ignored.
 * (doc: http://www.funducode.com/freec/Fileformats/format3/format3b.htm)
 */

public abstract class JPEGHeader {

    /**
     * prefix for all the markers
     **/
    public static final byte PREFIX = (byte) 0xff;
    /**
     * the SOI (Start of Image) marker
     **/
    public static final byte SOI = (byte) 0xd8;

    /**
     * the APP0 marker (JFIF marker)
     **/
    public static final byte JFIF = (byte) 0xe0;
    /**
     * the APP1 marker
     **/
    public static final byte APP1 = (byte) 0xe1;
    /**
     * the APP2 marker
     **/
    public static final byte APP2 = (byte) 0xe2;
    /**
     * the APP3 marker
     **/
    public static final byte APP3 = (byte) 0xe3;
    /**
     * the APP4 marker
     **/
    public static final byte APP4 = (byte) 0xe4;
    /**
     * the APP5 marker
     **/
    public static final byte APP5 = (byte) 0xe5;
    /**
     * the APP6 marker
     **/
    public static final byte APP6 = (byte) 0xe6;
    /**
     * the APP7 marker
     **/
    public static final byte APP7 = (byte) 0xe7;
    /**
     * the APP8 marker
     **/
    public static final byte APP8 = (byte) 0xe8;
    /**
     * the APP9 marker
     **/
    public static final byte APP9 = (byte) 0xe9;
    /**
     * the APP10 marker
     **/
    public static final byte APP10 = (byte) 0xea;
    /**
     * the APP11 marker
     **/
    public static final byte APP11 = (byte) 0xeb;
    /**
     * the APP12 marker
     **/
    public static final byte APP12 = (byte) 0xec;
    /**
     * the APP13 marker
     **/
    public static final byte APP13 = (byte) 0xed;
    /**
     * the APP14 marker
     **/
    public static final byte APP14 = (byte) 0xee;
    /**
     * the APP15 marker
     **/
    public static final byte APP15 = (byte) 0xef;

    /**
     * the SOF0 (Start of Frame) marker
     **/
    public static final byte SOF0 = (byte) 0xc0;
    /**
     * the SOF1 marker
     **/
    public static final byte SOF1 = (byte) 0xc1;
    /**
     * the SOF2 marker
     **/
    public static final byte SOF2 = (byte) 0xc2;
    /**
     * the SOF3 marker
     **/
    public static final byte SOF3 = (byte) 0xc3;
    /**
     * the SOF5 marker
     **/
    public static final byte SOF5 = (byte) 0xc5;
    /**
     * the SOF6 marker
     **/
    public static final byte SOF6 = (byte) 0xc6;
    /**
     * the SOF7 marker
     **/
    public static final byte SOF7 = (byte) 0xc7;
    /**
     * the SOF9 marker
     **/
    public static final byte SOF9 = (byte) 0xc9;
    /**
     * the SOF10 marker
     **/
    public static final byte SOF10 = (byte) 0xca;
    /**
     * the SOF11 marker
     **/
    public static final byte SOF11 = (byte) 0xcb;
    /**
     * the SOF13 marker
     **/
    public static final byte SOF13 = (byte) 0xcd;
    /**
     * the SOF14 marker
     **/
    public static final byte SOF14 = (byte) 0xce;
    /**
     * the SOF15 marker
     **/
    public static final byte SOF15 = (byte) 0xcf;

    /**
     * the DHT (Define Huffman Table) marker
     **/
    public static final byte DHT = (byte) 0xc4;
    /**
     * the DQT (Define Quantization Table) marker
     **/
    public static final byte DQT = (byte) 0xdb;
    /**
     * the SOS (Start of Scan) marker
     **/
    public static final byte SOS = (byte) 0xda;

    /**
     * the JPG marker
     **/
    public static final byte JPG = (byte) 0xc8;
    /**
     * the JPG0 marker
     **/
    public static final byte JPG0 = (byte) 0xf0;
    /**
     * the JPG1 marker
     **/
    public static final byte JPG1 = (byte) 0xf1;
    /**
     * the JPG2 marker
     **/
    public static final byte JPG2 = (byte) 0xf2;
    /**
     * the JPG3 marker
     **/
    public static final byte JPG3 = (byte) 0xf3;
    /**
     * the JPG4 marker
     **/
    public static final byte JPG4 = (byte) 0xf4;
    /**
     * the JPG5 marker
     **/
    public static final byte JPG5 = (byte) 0xf5;
    /**
     * the JPG6 marker
     **/
    public static final byte JPG6 = (byte) 0xf6;
    /**
     * the JPG7 marker
     **/
    public static final byte JPG7 = (byte) 0xf7;
    /**
     * the JPG8 marker
     **/
    public static final byte JPG8 = (byte) 0xf8;
    /**
     * the JPG9 marker
     **/
    public static final byte JPG9 = (byte) 0xf9;
    /**
     * the JPG10 marker
     **/
    public static final byte JPG10 = (byte) 0xfa;
    /**
     * the JPG11 marker
     **/
    public static final byte JPG11 = (byte) 0xfb;
    /**
     * the JPG12 marker
     **/
    public static final byte JPG12 = (byte) 0xfc;
    /**
     * the JPG13 marker
     **/
    public static final byte JPG13 = (byte) 0xfd;

    /**
     * the DAC (Define Arithmetic Table) marker
     **/
    public static final byte DAC = (byte) 0xcc;
    /**
     * the DNL marker
     **/
    public static final byte DNL = (byte) 0xdc;
    /**
     * the DRI (Define Restart Interval) marker
     **/
    public static final byte DRI = (byte) 0xdd;
    /**
     * the DHP marker
     **/
    public static final byte DHP = (byte) 0xde;
    /**
     * the EXP marker
     **/
    public static final byte EXP = (byte) 0xdf;

    /**
     * the RST0 (Resync Table) marker
     **/
    public static final byte RST0 = (byte) 0xd0;
    /**
     * the RST1 marker
     **/
    public static final byte RST1 = (byte) 0xd1;
    /**
     * the RST2 marker
     **/
    public static final byte RST2 = (byte) 0xd2;
    /**
     * the RST3 marker
     **/
    public static final byte RST3 = (byte) 0xd3;
    /**
     * the RST4 marker
     **/
    public static final byte RST4 = (byte) 0xd4;
    /**
     * the RST5 marker
     **/
    public static final byte RST5 = (byte) 0xd5;
    /**
     * the RST6 marker
     **/
    public static final byte RST6 = (byte) 0xd6;
    /**
     * the RST7 marker
     **/
    public static final byte RST7 = (byte) 0xd7;

    /**
     * the TEM marker
     **/
    public static final byte TEM = (byte) 0x01;
    /**
     * the comment marker
     **/
    public static final byte COM = (byte) 0xfe;
    /**
     * the EOI (End of Image) marker
     **/
    public static final byte EOI = (byte) 0xd9;
    /**
     * the JFIF identifier in the APP0 marker segment
     **/
    public static final byte[] JFIF_ID = {(byte) 0x4a, (byte) 0x46, (byte) 0x49, (byte) 0x46, (byte) 0x00}; //"JFIF"#0

    /* the maximum size of a segment (default 65535) */
    public static final int max_segm_size = 65535;

}