package jevc.utils;

import java.io.*;

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
 * 	.Following the SOI marker, there can be any number of ?segments? or ?markers? such as:
 *	 APP0..APP15, SOF0..SOF15, DQT, DHT, SOS, JPG, JPG0..JPG13, DAC, DNL, DRI, DHP, EXP, RST0..RST7, TEM, COM.
 *      .An APP0 segment immediately follows the SOI marker.
 * -Trailer:
 * 	.It occupies two bytes.
 *      .0xff, 0xd9 (EOI: End of Image) (these two identify end of image).
 *
 * Note: any number of 0xff bytes between two segments (markers) must be ignored.
 * (doc: http://www.funducode.com/freec/Fileformats/format3/format3b.htm)
 */
public class JPGInfoDump {

    /** prefix for all the markers **/
    private static final byte PREFIX=(byte) 0xff;
    /** the SOI (Start of Image) marker **/
    private static final byte SOI = (byte) 0xd8;

    /** the APP0 marker (JFIF marker) **/
    private static final byte JFIF = (byte) 0xe0;
    /** the APP1 marker **/
    private static final byte APP1 = (byte) 0xe1;
    /** the APP2 marker **/
    private static final byte APP2 = (byte) 0xe2;
    /** the APP3 marker **/
    private static final byte APP3 = (byte) 0xe3;
    /** the APP4 marker **/
    private static final byte APP4 = (byte) 0xe4;
    /** the APP5 marker **/
    private static final byte APP5 = (byte) 0xe5;
    /** the APP6 marker **/
    private static final byte APP6 = (byte) 0xe6;
    /** the APP7 marker **/
    private static final byte APP7 = (byte) 0xe7;
    /** the APP8 marker **/
    private static final byte APP8 = (byte) 0xe8;
    /** the APP9 marker **/
    private static final byte APP9 = (byte) 0xe9;
    /** the APP10 marker **/
    private static final byte APP10 = (byte) 0xea;
    /** the APP11 marker **/
    private static final byte APP11 = (byte) 0xeb;
    /** the APP12 marker **/
    private static final byte APP12 = (byte) 0xec;
    /** the APP13 marker **/
    private static final byte APP13 = (byte) 0xed;
    /** the APP14 marker **/
    private static final byte APP14 = (byte) 0xee;
    /** the APP15 marker **/
    private static final byte APP15 = (byte) 0xef;

    /** the SOF0 (Start of Frame) marker **/
    private static final byte SOF0 = (byte) 0xc0;
    /** the SOF1 marker **/
    private static final byte SOF1 = (byte) 0xc1;
    /** the SOF2 marker **/
    private static final byte SOF2 = (byte) 0xc2;
    /** the SOF3 marker **/
    private static final byte SOF3 = (byte) 0xc3;
    /** the SOF5 marker **/
    private static final byte SOF5 = (byte) 0xc5;
    /** the SOF6 marker **/
    private static final byte SOF6 = (byte) 0xc6;
    /** the SOF7 marker **/
    private static final byte SOF7 = (byte) 0xc7;
    /** the SOF9 marker **/
    private static final byte SOF9 = (byte) 0xc9;
    /** the SOF10 marker **/
    private static final byte SOF10 = (byte) 0xca;
    /** the SOF11 marker **/
    private static final byte SOF11 = (byte) 0xcb;
    /** the SOF13 marker **/
    private static final byte SOF13 = (byte) 0xcd;
    /** the SOF14 marker **/
    private static final byte SOF14 = (byte) 0xce;
    /** the SOF15 marker **/
    private static final byte SOF15 = (byte) 0xcf;

    /** the DHT (Define Huffman Table) marker **/
    private static final byte DHT = (byte) 0xc4;
    /** the DQT (Define Quantization Table) marker **/
    private static final byte DQT = (byte) 0xdb;
    /** the SOS (Start of Scan) marker **/
    private static final byte SOS = (byte) 0xda;

    /** the JPG marker **/
    private static final byte JPG = (byte) 0xc8;
    /** the JPG0 marker **/
    private static final byte JPG0 = (byte) 0xf0;
    /** the JPG1 marker **/
    private static final byte JPG1 = (byte) 0xf1;
    /** the JPG2 marker **/
    private static final byte JPG2 = (byte) 0xf2;
    /** the JPG3 marker **/
    private static final byte JPG3 = (byte) 0xf3;
    /** the JPG4 marker **/
    private static final byte JPG4 = (byte) 0xf4;
    /** the JPG5 marker **/
    private static final byte JPG5 = (byte) 0xf5;
    /** the JPG6 marker **/
    private static final byte JPG6 = (byte) 0xf6;
    /** the JPG7 marker **/
    private static final byte JPG7 = (byte) 0xf7;
    /** the JPG8 marker **/
    private static final byte JPG8 = (byte) 0xf8;
    /** the JPG9 marker **/
    private static final byte JPG9 = (byte) 0xf9;
    /** the JPG10 marker **/
    private static final byte JPG10 = (byte) 0xfa;
    /** the JPG11 marker **/
    private static final byte JPG11 = (byte) 0xfb;
    /** the JPG12 marker **/
    private static final byte JPG12 = (byte) 0xfc;
    /** the JPG13 marker **/
    private static final byte JPG13 = (byte) 0xfd;

    /** the DAC (Define Arithmetic Table) marker **/
    private static final byte DAC = (byte) 0xcc;
    /** the DNL marker**/
    private static final byte DNL = (byte) 0xdc;
    /** the DRI (Define Restart Interval) marker **/
    private static final byte DRI = (byte) 0xdd;
    /** the DHP marker **/
    private static final byte DHP = (byte) 0xde;
    /** the EXP marker **/
    private static final byte EXP = (byte) 0xdf;

    /** the RST0 (Resync Table) marker **/
    private static final byte RST0 = (byte) 0xd0;
    /** the RST1 marker **/
    private static final byte RST1 = (byte) 0xd1;
    /** the RST2 marker **/
    private static final byte RST2 = (byte) 0xd2;
    /** the RST3 marker **/
    private static final byte RST3 = (byte) 0xd3;
    /** the RST4 marker **/
    private static final byte RST4 = (byte) 0xd4;
    /** the RST5 marker **/
    private static final byte RST5 = (byte) 0xd5;
    /** the RST6 marker **/
    private static final byte RST6 = (byte) 0xd6;
    /** the RST7 marker **/
    private static final byte RST7 = (byte) 0xd7;

    /** the TEM marker **/
    private static final byte TEM = (byte) 0x01;
    /** the comment marker **/
    private static final byte COM = (byte) 0xfe;
    /** the EOI (End of Image) marker **/
    private static final byte EOI = (byte) 0xd9;
    /** the JFIF identifier in the APP0 marker segment **/
    private static final byte[] JFIF_ID={(byte) 0x4a,(byte) 0x46,(byte) 0x49,(byte) 0x46,(byte) 0x00}; //"JFIF"#0

    /* the maximum size of a segment (default 65535) */
    public static final int max_segm_size=65535;

    private RandomAccessFile fjpg;

    public JPGInfoDump(String file) {
		try {
			/* open the .jpg file */
			fjpg=new RandomAccessFile(file,"r");
		} catch(IOException ex) {System.err.println(ex.getMessage());}
    }

    public void dumpHeader() {
		byte val = 0,b1,b2;
		byte buffer[] = new byte[max_segm_size];
		int i,j,x,size;
		int sos = 0;
		i = 0;
		try {
			/* read the SOI */
			fjpg.readFully(buffer,0,2);
			if(buffer[0]!=PREFIX || buffer[1]!=SOI) {
				System.err.println("The file does not start with SOI.");
				return;
			}
			System.out.println("SOI --------------------------------------------------------");

			/* read JFIF segment (APP0 marker) */
			/**
			 * Structure of the JFIF segment:
			 * - JFIF marker (2 bytes)
			 * - length of the segment without JFIF marker and including this 2-byte length field (2 bytes)
			 * - zero terminated string "JFIF" that uniquely identifies this APP0 marker (5 bytes) : 0x4a, 0x46, 0x49, 0x46, 0x00
			 * - JFIF version (2 bytes)
			 * - units for X and Y density (1 byte) - 0 for pixels
			 * - X density (2 bytes)
			 * - Y density (2 bytes)
			 * - Xthumbnail: thumbnail horizontal pixel count (1 byte)
			 * - Ythumbnail: thumbnail vertical pixel count (1 byte)
			 * - packed RGB values for the thumbnail pixels (3*Xthumbnail*Ythumbnail bytes)
			 */
			fjpg.readFully(buffer,0,4);
			if(buffer[0]!=PREFIX || buffer[1]!=JFIF) {
				System.err.println("The file does not have a APP0 marker segment.");
				return;
			}
			i = buffer[2]<<8;
			i |= buffer[3];		// i contains the length of the JFIF marker segment
			j = 1;
			System.out.println("        APP0 marker segment (length="+i+") :");

			/* read the JFIF0 identifier */
			fjpg.readFully(buffer,0,5);
			j += 5;
			if(!equal(buffer, JFIF_ID, 5)) {
				System.err.println("The APP0 marker segment does not have the identifier \"JFIF#0\".");
				return;
			}
			System.out.println("        	identifier: "+new String(buffer,0,5));

			/* read the JFIF version */
			fjpg.readFully(buffer,0,2);
			j += 2;
			System.out.println("        	JFIF version:"+(int)buffer[0]+"."+(int)buffer[1]);

			/* read the units */
			val = fjpg.readByte();
			j += 1;
			System.out.println("        	units:"+(int)val);

			/* read X density */
			fjpg.readFully(buffer,0,2);
			j += 2;
			x = buffer[0]<<8;
			x |= buffer[1];
			System.out.println("        	horizontal pixel density:"+x);

			/* read Y density */
			fjpg.readFully(buffer,0,2);
			j += 2;
			x = buffer[0]<<8;
			x |= buffer[1];
			System.out.println("        	vertical pixel density:"+x);

			/* read X thumbnail */
			val = fjpg.readByte();
			j += 1;
			x = (int)val;
			System.out.println("        	thumbnail horizontal pixel count:"+(int)val);

			/* read Y thumbnail */
			val = fjpg.readByte();
			j += 1;
			x *= (int)val;
			System.out.println("        	thumbnail vertical pixel count:"+(int)val);

			/* packed RGB values for the thumbnail pixels (3*Xthumbnail*Ythumbnail), if are there any */
			if (x>0) {
				fjpg.readFully(buffer,0,3*x);
				j += 3*x;
				}
			if (j!=i-1) {System.err.println("Corrupted JFIF segment."); return; }

			/* read segments or markers */
			fjpg.readFully(buffer,0,1);
			if (buffer[0]!=PREFIX) {
				System.err.println("Unrecognized prefix for marker:"+buffer[0]);
				return;
			}
			/* we keep reading segments until we reach the compressed image data, that is after we parse the SOS segment */
			while (sos==0) {
				while (buffer[0]==PREFIX) {
					fjpg.readFully(buffer,0,1);
					/*System.out.println(printHex(buffer[0]));*/
					/* if a 0x00 byte follows a 0xff byte, the zero byte (0x00) must be discarded.
					 * this happens only in the compressed image data, following the SOS marker,
					 * which we don't parse, but just in case */
					if (buffer[0]==0x00) fjpg.readFully(buffer,0,1);
				}
				i = 1;
				/**
				 * Depending on the marker found in buffer[0], we take two kinds of actions:
				 * -if buffer[0] != SOS, we read bytes until we find a PREFIX (which means that a new segment starts)
				 * -if buffer[0] == SOS, this means that this is the last segment from the JFIF file and immediately
				 *  after it the compressed image data follows and then the [0xff,0xd9] marker which completes the
				 *  JFIF file. In this case, we determine the size of the segment by reading the first two bytes
				 *  following the segment marker (buffer[0]) and than we read as many bytes as the calculated size
				 *  tells us to.
				 */
				if (buffer[0] != SOS) {
					while (buffer[i - 1] != PREFIX) buffer[i++] = fjpg.readByte();
				} else {
					/* read the SOS segment, the last segment before the image date */
					fjpg.readFully(buffer,1,2);
					i += 2;
					size = ((int) buffer[1]) & 0xff;
					size <<= 8;
					size |= ((int) buffer[2]) & 0xff;
					fjpg.readFully(buffer,i,size-2);
					i += size-2;
				}
				//System.out.println("A new marker ("+printHex(buffer[0])+") *****************************************************************************************************");
				//System.out.println(printHex(buffer[1])+printHex(buffer[2])+new String(buffer,3,i-4)+"***");
				//System.out.println("End of marker *****************************************************************************************************************");
				switch(buffer[0]) {
					case APP1:
						dumpAPPsegment("APP1",buffer,i-1);
						break;
					case APP2:
						dumpAPPsegment("APP2",buffer,i-1);
						break;
					case APP3:
						dumpAPPsegment("APP3",buffer,i-1);
						break;
					case APP4:
						dumpAPPsegment("APP4",buffer,i-1);
						break;
					case APP5:
						dumpAPPsegment("APP5",buffer,i-1);
						break;
					case APP6:
						dumpAPPsegment("APP6",buffer,i-1);
						break;
					case APP7:
						dumpAPPsegment("APP7",buffer,i-1);
						break;
					case APP8:
						dumpAPPsegment("APP8",buffer,i-1);
						break;
					case APP9:
						dumpAPPsegment("APP9",buffer,i-1);
						break;
					case APP10:
						dumpAPPsegment("APP10",buffer,i-1);
						break;
					case APP11:
						dumpAPPsegment("APP11",buffer,i-1);
						break;
					case APP12:
						dumpAPPsegment("APP12",buffer,i-1);
						break;
					case APP13:
						dumpAPPsegment("APP13",buffer,i-1);
						break;
					case APP14:
						dumpAPPsegment("APP14",buffer,i-1);
						break;
					case APP15:
						dumpAPPsegment("APP15",buffer,i-1);
						break;
					case SOF0:
						dumpSOFsegment("SOF0",buffer,i-1);
						break;
					case SOF1:
						dumpSOFsegment("SOF1",buffer,i-1);
						break;
					case SOF2:
						dumpSOFsegment("SOF2",buffer,i-1);
						break;
					case SOF3:
						dumpSOFsegment("SOF3",buffer,i-1);
						break;
					case SOF5:
						dumpSOFsegment("SOF5",buffer,i-1);
						break;
					case SOF6:
						dumpSOFsegment("SOF6",buffer,i-1);
						break;
					case SOF7:
						dumpSOFsegment("SOF7",buffer,i-1);
						break;
					case SOF9:
						dumpSOFsegment("SOF9",buffer,i-1);
						break;
					case SOF10:
						dumpSOFsegment("SOF10",buffer,i-1);
						break;
					case SOF11:
						dumpSOFsegment("SOF11",buffer,i-1);
						break;
					case SOF13:
						dumpSOFsegment("SOF13",buffer,i-1);
						break;
					case SOF14:
						dumpSOFsegment("SOF14",buffer,i-1);
						break;
					case SOF15:
						dumpSOFsegment("SOF15",buffer,i-1);
						break;
					case DHT:
						dumpDHTsegment(buffer,i-1);
						break;
					case DQT:
						dumpDQTsegment(buffer,i-1);
						break;
					case JPG:
						dumpJPGsegment("JPG",buffer,i-1);
						break;
					case JPG0:
						dumpJPGsegment("JPG0",buffer,i-1);
						break;
					case JPG1:
						dumpJPGsegment("JPG1",buffer,i-1);
						break;
					case JPG2:
						dumpJPGsegment("JPG2",buffer,i-1);
						break;
					case JPG3:
						dumpJPGsegment("JPG3",buffer,i-1);
						break;
					case JPG4:
						dumpJPGsegment("JPG4",buffer,i-1);
						break;
					case JPG5:
						dumpJPGsegment("JPG5",buffer,i-1);
						break;
					case JPG6:
						dumpJPGsegment("JPG6",buffer,i-1);
						break;
					case JPG7:
						dumpJPGsegment("JPG7",buffer,i-1);
						break;
					case JPG8:
						dumpJPGsegment("JPG8",buffer,i-1);
						break;
					case JPG9:
						dumpJPGsegment("JPG9",buffer,i-1);
						break;
					case JPG10:
						dumpJPGsegment("JPG10",buffer,i-1);
						break;
					case JPG11:
						dumpJPGsegment("JPG11",buffer,i-1);
						break;
					case JPG12:
						dumpJPGsegment("JPG12",buffer,i-1);
						break;
					case JPG13:
						dumpJPGsegment("JPG13",buffer,i-1);
						break;
					case DAC:
						dumpDACsegment(buffer,i-1);
						break;
					case DNL:
						dumpDNLsegment(buffer,i-1);
						break;
					case DRI:
						dumpDRIsegment(buffer,i-1);
						break;
					case DHP:
						dumpDHPsegment(buffer,i-1);
						break;
					case EXP:
						dumpEXPsegment(buffer,i-1);
						break;
					case RST0:
						dumpRSTsegment("RST0",buffer,i-1);
						break;
					case RST1:
						dumpRSTsegment("RST1",buffer,i-1);
						break;
					case RST2:
						dumpRSTsegment("RST2",buffer,i-1);
						break;
					case RST3:
						dumpRSTsegment("RST3",buffer,i-1);
						break;
					case RST4:
						dumpRSTsegment("RST4",buffer,i-1);
						break;
					case RST5:
						dumpRSTsegment("RST5",buffer,i-1);
						break;
					case RST6:
						dumpRSTsegment("RST6",buffer,i-1);
						break;
					case RST7:
						dumpRSTsegment("RST7",buffer,i-1);
						break;
					case TEM:
						dumpTEMsegment(buffer,i-1);
						break;
					case COM:
						dumpCOMsegment(buffer,i-1);
						break;
					case SOS:
						dumpSOSsegment(buffer,i);
						sos = 1;
						break;
					default:
						System.err.println("Marker not recognized.");
				}
				buffer[0]=PREFIX; // put the last byte read (PREFIX) in the first element of the buffer

			}
			/* Dump compressed image data (the rest of the bytes until the two EOI bytes that end the .jpg file) */
			System.out.println("Image data (scans) length: " + (fjpg.length()-2-fjpg.getFilePointer()));
			System.out.println("The compressed bitstream follows:");
			System.out.println("========================================================================");
			i = 0;
			while (true) {
				val = (byte) fjpg.readUnsignedByte();
				i++;
				buffer[0] = buffer[1];
				buffer[1] = val;  // we store the last two read bytes
				System.out.print(printHex(val) + " ");
				if (i%8 == 0) System.out.println();
			}

		} catch(EOFException e) {
			// check if the last two bytes from the file are PREFIX,EOI
			if(buffer[0]==PREFIX && buffer[1]==EOI) {
				System.out.println("\nThe size of the compressed image data is: " + (i - 2) + " bytes.");
				System.out.println("EOI --------------------------------------------------------");
			} else {
				System.out.println("Warning: could not find the EOI marker!");
			}
			//System.err.println("EOF reached.");
		} catch(IOException e) {System.err.println("Error: " + e.getMessage());}

    }

    private boolean equal(byte[] first,byte[] second,int length) {
	for(int i=0; i<length; i++)
		if(first[i]!=second[i]) return false;
	return true;
    }

    /**
     * Dumps on the screen the content of a given APP (Application) segment.
     * Since the APP segments are application dependent, we only print the
     * type of the segment (APP1,APP2,APP3,APP4,APP5,APP6,APP7,APP8,APP9,
     * APP10,APP11,APP12,APP13,APP14,APP15) and its length. The type of the
     * APP segment is specified by the type parameter (also, the type is
     * specified by buffer[0]).
     * @param type the type of the APP segment
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void dumpAPPsegment(String type,byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the APPn marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if (size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        "+type+" segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the content of a given SOF (Start of Frame) segment.
     * We only print the content of a SOF0 segment, because the others are usually
     * not supported.
     * @param type the type of the SOF segment. Must be oane of: SOF0,SOF1,SOF2,
     *		   SOF3,SOF5,SOF6,SOF7,SOF9,SOF10,SOF11,SOF13,SOF14,SOF15
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void dumpSOFsegment(String type,byte[] buffer,int len) {
    	int x,z;
		if(type!=null && type.equals("SOF0")) {
			System.out.println("        "+type+" segment encountered:");
			/** the structure of the SOF0 segment is:
			 * -the SOF0 marker (0xff,0xc0) - 2 bytes (buffer starts only from 0xc0, so buffer[0]=0xc0)
			 * -the length of the segment (2 bytes) without the SOF0 marker, but including these
			 *   2 bytes for representing the length - equal to 8+component*3
			 * -data precision (1 byte) - the bits/sample, usually 8 or 12
			 * -image height (2 bytes) - must be >0
			 * -image width (2 bytes) - must be >0
			 * -number of components (1 byte) - usually 1 (gray scaled), 3 (YUV or YIQ), 4 (CMYK)
			 * -each component is described by 3 bytes of data:
			 *	.id (1 byte): 1=Y, 2=Cb, 3=Cr, 4=I, 5=Q
			 *	.sampling factors (1 byte): bits 0-3 vertical, 4-7 horizontal
			 *	.quantization table number (1 byte)
			 */
			x = ((int) buffer[1]) & 0xff;
			x <<= 8;
			x |= ((int) buffer[2] & 0xff);
			System.out.println("        	.length="+x);
			/* check if size==len -1 */
			if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
			x = ((int) buffer[3]) & 0xff;
			System.out.println("        	.data precision (bits/sample)="+x);
			x = ((int) buffer[4]) & 0xff;
			x <<= 8;
			x |= ((int) buffer[5] & 0xff);
			System.out.println("        	.image height = "+x);
			x = ((int) buffer[6]) & 0xff;
			x <<= 8;
			x |= ((int) buffer[7] & 0xff);
			System.out.println("        	.image width = "+x);
			switch(buffer[8]) {
				case 1:
					System.out.println("        	.the image is gray scaled");
					break;
				case 3:
					System.out.println("        	.the image is in YCbCr or YIQ format");
					break;
				case 4:
					System.out.println("        	.the image is in CMYK format");
					break;
				default:
					System.out.println("        	.the image has "+buffer[8]+" components");
					break;
			}
			System.out.println("        	.the image components (id= 1(Y), 2(Cb), 3(Cr), 4(I), 5(Q))");
			for(int i=9; i<=len-3; i+=3) {
				x=((int) buffer[i+1]) & 0x0f;
				z=((int) buffer[i+1]) & 0xf0;
				z >>= 4;
				System.out.println("        		*id="+buffer[i]+",vert_sampl_factor="+x+" horiz_sampl_factor="+z+",Q table no.="+buffer[i+2]);
			}
			System.out.println("        End of SOF0 segment");
		} else {
			/* we only print the type of the SOF segment and its size*/
			x = ((int) buffer[1]) & 0xff;
			x <<= 8;
			x |= ((int) buffer[2] & 0xff);
			/* check if size==len -1 */
			if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
			System.out.println("        "+type+" segment encountered (length="+x+").");
		}
    }

    /**
     * Dumps on the screen the content of a given JPG segment. We only print
     * the type of the segment (JPG, JPG0 .. JPG13) and its length. The type
     * of the JPG segment is specified by the type parameter (also, the type
     * is specified by buffer[0]).
     * @param type the type of this JPG segment
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void dumpJPGsegment(String type,byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the JPG marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        "+type+" segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DAC (Define Arithmetic Table)
     * segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDACsegment(byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the DAC marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        Define Arithmetic Table segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DNL segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDNLsegment(byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the DNL marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        DNL segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DHP segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDHPsegment(byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the DHP marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        DHP segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the EXP segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpEXPsegment(byte[] buffer,int len) {
    	int size;
		/* the size of the segment without the EXP marker is contained in buffer[1]
		 * and buffer[2](in big endian format - big byte first) */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        EXP segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen a RSTn segment. Since this kind of segment
     * does not have 2 bytes for its size, we just print len-1 as
     * its size and hope for the best:)
     * @param type the type of this RST segment. Must be one of
     * 		   RST0,RST1,RST2,RST3,RST4,RST5,RST6,RST7.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpRSTsegment(String type,byte[] buffer,int len) {
		System.out.println("        "+type+" segment encountered (length="+(len-1)+").");
    }

    /**
     * Dumps on the screen a TEM segment. Since this kind of segment
     * does not have 2 bytes for its size, we just print len-1 as
     * its size and hope for the best:)
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpTEMsegment(byte[] buffer,int len) {
		System.out.println("        TEM segment encountered (length="+(len-1)+").");
    }

    /**
     * Dumps on the screen a COM (Comment) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpCOMsegment(byte[] buffer,int len) {
		int size;
		/**
		 * the structure of the COM segment is:
		 * -the COM marker (0xff,0xfe) - 2 bytes (buffer starts only from 0xfe, so buffer[0]=0xfe)
		 * -the length of the segment (2 bytes) without the COM marker, but including these
		 *   2 bytes for representing the length
		 * -the comment itself as a sequence of length-2 bytes
		 */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2]) & 0xff;
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		//System.out.println("COM: size="+size+" len="+len);
		System.out.println("        COM segment encountered (length="+size+"): "+new String(buffer,3,size-2));
    }

    /**
     * Dumps on the screen the content of a DRI (Define Restart Interval) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDRIsegment(byte[] buffer,int len) {
		/**
		 * the structure of the DRI segment is:
		 * -the DRI marker (0xff,0xdd) - 2 bytes (buffer starts only from 0xdd, so buffer[0]=0xdd)
		 * -the length of the segment (2 bytes) without the DRI marker, but including these
		 *   2 bytes for representing the length - it must be 4
		 * -restart interval (2 bytes) - this is in units of MCU blocks, means that every n MCU
			 *   blocks a RSTn marker can be found. The first marker will be RST0, then RST1 etc,
		 *   after RST7 repeating from RST0
		 */
		int x,y;
		x = ((int) buffer[1]) & 0xff;
		x <<= 8;
		x |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		y = ((int) buffer[3]) & 0xff;
		y <<= 8;
		y |= ((int) buffer[4] & 0xff);
		if(x!=4) System.out.println("        DRI segment encountered but is corrupted (length="+x+").");
		else System.out.println("        DRI segment encountered: length="+x+" restart interval="+y);
    }

    /**
     * Dumps on the screen the content of a DQT (Define Quantization Table) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDQTsegment(byte[] buffer,int len) {
		/**
		 * the structure of the DQT segment is:
		 * -the DQT marker (0xff,0xdb) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xdb)
		 * -length (2 bytes) of the DQT segment without the DQT marker, but including the bytes
		 *   for this field
		 * -multiple QT tables, each of them containing:
		 *	.QT information, 1 byte: bit 0..3 - number of this QT (0,1,2,3)
		 *			         bit 4..7 - precision of QT (0 = 8bit, otherwise = 16bit)
		 *	.QT bytes, 64*(precision+1) bytes: the QT values
		 */
		int size,i,j,no,precision,value_width,val;
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        DQT segment encountered (length="+size+")");
		System.out.println("        	.The Quantization Tables in ZIGZAG order:");
		//System.out.println("size="+(size-2)+" length="+(len-3));
		i=3;
		while(i<=size && i<len) {
			/* print one Q table */
			/* determine the no. and precision for this QT */
			no = ((int) buffer[i]) & 0x0f;
			precision = ((int) buffer[i]) & 0xf0;
			System.out.println("        	Q TABLE no. "+no+", precision "+(precision==0?"8bit":"16bit")+":");
			/* set the width of a Q value (1 byte or 2 bytes) */
			if(precision==0) value_width=1;
			else value_width=2;
			/* print the Q values */
			System.out.print("        	");
			/* this code is rather complicated because I wanted to catch both cases (precision=0 & precision=1) in one for run */
			for(j=i+1; j<64*value_width+i+1 && (j<=(size+1)-value_width && j<=len-value_width); j+=value_width) {
				if(precision==0) val=((int) buffer[j]) & 0xff;
				else {
					val = ((int) buffer[j] <<8 ) & 0xff00;
					val |=((int) buffer[j+1]) & 0xff;
				}
				System.out.print(val+",  ");
				/* line up the new Q table */
				if(j!=i+1 && (j+value_width-(i+1))%(value_width*8)==0) { System.out.println(); System.out.print("                ");}
			}
			i=j;
			System.out.println();
		}
		System.out.println("        DQT segment end.");
    }

    /**
     * Dumps on the screen the content of a DHT (Define Huffman Table) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDHTsegment(byte[] buffer,int len) {
		/**
		 * the structure of the DHT segment is:
		 * -the DHT marker (0xff,0xc4) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xc4)
		 * -length (2 bytes) of the DHT segment without the DHT marker, but including the bytes
		 *   for this field
		 * -multiple HT tables, each of them containing:
		 *	.HT information, 1 byte: bit 0..3 - number of this HT (0,1,2,3)
		 *				 bit 4 - type of this HT (0=DC table, 1=AC table)
		 *				 bit 5..7 - not used, must be zero
		 *	.Number of symbols, 16 bytes: number of symbols with codes of length 1,2..16.
		 *				      The sum (let this sum be n) of these bytes is the
		 *				      total number of codes, which must be <=256
		 *	.Symbols, n bytes (see above): table containing the symbols in order of increasing
		 *				       code length (n=total number of codes)
		 */
		int size,i,j,k,x,no,type/* type=0 -DC || type=1 -AC */;
		int n;
		byte symb_no[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // symb_no[i] is the no. of symbols with codes of length i
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2]) & 0xff;
		/* check if size==len -1 */
		if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
		System.out.println("        DHT segment encountered (length="+size+")");
		System.out.println("        	.The Huffman Tables are:");
		i=3;
		while(i<=size && i<len) {
			/* print one H table */
			/* determine no. and type for this HT */
			no = ((int) buffer[i]) & 0x0f;
			type = ((int) buffer[i]) & 0x10;
			type >>= 4;
			x = ((int) buffer[i]) & 0xe0;
			x >>= 5;
			if(x!=0) {
				System.out.println("        	Corrupted Huffman table!");
				return;
			}
			System.out.println("        	H table no. "+no+" type "+(type==0?"DC":"AC")+":");
			i += 1;
			/* fill the symb_no array with proper values */
			n = 0;
			System.out.print("            Number of symbols:");
			for(j=0; j<16 && (i+j)<len ; j++) {
					symb_no[j]=buffer[i+j];
					n += symb_no[j];
					System.out.print("(" + symb_no[j] + " symbs. with code of length " + (j+1) + ") ");
			}
			System.out.println();

			if(n>256) {System.out.println("            The no. ofs ymbols in HT is >256!"); return;}
			i += j;
			/* reading the symbols (n bytes) */
			for(j=0; j<16; j++) {
				System.out.print("        	");
				if(symb_no[j]==0) System.out.print("-");
				for(k=0; k<symb_no[j] && (i<len && i<=size); k++) {
					/* reading symb_no[j] of length j from buffer */
					System.out.print(printHex(buffer[i])+",  ");
					i++;
				}
				System.out.println();
			}

		}
		System.out.println("        DHT segment end.");
    }

    /**
     * Dumps on the screen the content of a SOS (Start of Scan) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
		private void dumpSOSsegment(byte[] buffer,int len) {
		/**
		 * the structure of a SOS segment:
		 * -the SOS marker (0xff,0xda) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xda)
		 * -length (2 bytes) of the SOS segment without the SOS marker, but including the bytes
		 *   for this field - must be equal to 6+2*(no. of components in scan)
		 * -number of components in scan (1 byte) - must be 1,2,3 or 4, otherwise error
		 * -each component (2 bytes):
		 *	.component ID, 1 byte: (1=Y, 2=Cb, 3=Cr, 4=I, 5=Q)
		 *	.Huffman tables to use, 1 byte: bit 0..3 - AC table (0..3)
		 *					bit 4..7 - DC table (0..3)
		 * -ignorable bytes (3 bytes):
		 *	.start of spectral selection (1 byte)
		 *	.end of spectral selection (1 byte)
		 *	.1 ignorable byte (?)
		 *
		 * After the SOS segment, the compressed bits of the image follow (i.e. the scan pixels)
		 * until we get to the two EOI bytes (0xff 0xd9) that complete the file.
		 */
		int size,no,i,j,id,huff,dc,ac,ss,es;
		/* compute size */
		size = ((int) buffer[1]) & 0xff;
		size <<= 8;
		size |= ((int) buffer[2] & 0xff);
		no = ((int) buffer[3]) & 0xff;
		System.out.println("        SOS (Start of Scan) segment encountered: length "+size+" no. of components "+no);
		/* compute number of components */
		if(size!=6+2*no) {System.err.println("        SOS segment is corrupted!"); return;}
		i = 4;
		/* read each component */
		for(j=0; j<no && (i<size-1 && i<len-1); j++) {
			id=((int) buffer[i]) & 0xff;
			huff=((int) buffer[i+1]) & 0xff;
			i += 2;
			dc = (huff & 0xf0) >>4;
			ac = (huff & 0x0f);
			System.out.println("        	.Component "+id+" AC table="+ac+" DC table="+dc);
		}
		/* if those 3 ignorable bytes are not present, error? */
		if(i+3 != len) System.out.println("        SOS segment corrupted.");
		ss = ((int) buffer[i]) & 0xff;
		es = ((int) buffer[i+1]) & 0xff;
		System.out.println("        	.Start of spectral selection: "+ss);
		System.out.println("        	.End of spectral selection: "+es);
		System.out.println("        End of SOS segment");
    }

    /**
     * A function that returns a byte's representation in hex format
     * (because I couldn't find one in the millions of java classes from the jdk)
     */
    private String printHex(byte b) {
    	char[] digits={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		char s[]=new char[4];
		int i,j;
		i = ((int) b) & 0xf0;
		i >>= 4;
		j = ((int) b) & 0x0f;
		s[0]='0'; s[1]='x'; s[2]=digits[i]; s[3]=digits[j];
		return new String(s);
    }

    /*-------------------------- test method --------------------------*/
    public static void main (String args[]) {
	JPGInfoDump jinfo=new JPGInfoDump(args[0]);
	jinfo.dumpHeader();
    }

}


