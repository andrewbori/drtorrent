package hu.bute.daai.amorg.drtorrent.logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Logger {
    public static String logFile = "/sdcard/DrTorrentLog.txt";


    private static boolean EnableLog = true;
    private static OutputStream out = null;
    private final static Object sync = new Object();

    private static OutputStream getStream()
    {
    	if(out == null)
    	{
    		try {
				out = new FileOutputStream(logFile);
			} catch (FileNotFoundException e) {
			}
    	}
        return out;
    }

    public static void close()
    {
        try
        {
            out.close();
        }
        catch(Exception e) {
        	
        }

    }

    /** Writes a string to the log file and closes the line */
    public static void writeLine(String aData)
    {
        if(aData == null)
        {
            writeLine("\n");
        }
        else
        {
            write(aData + '\n');
        }
    }

    /** Writes a byte[] to the log file and closes the line */
    public static void writeLine(byte[] aData)
    {
        if (EnableLog)
        {
            try
            {
                getStream().write(aData);
                getStream().flush();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }

        }
    }
    /** Writes an empty line */
    public static void writeLine()
    {
        write("\n");
    }

    /** Appends a string to the log file */
    public static void write(String aData)
    {
        if(!EnableLog)
            return;

        synchronized(sync)
        {
                try
                {
                    getStream().write((aData).getBytes());
                    getStream().flush();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
        }
    }

    /** Writes an int to the log file and closes the line */
    public static void writeLine(int aData)
    {
        write(String.valueOf(aData) + '\n');
    }

    /** Appends an int to the log file */
    public static void write(int aData)
    {
        write(String.valueOf(aData));
    }

    final private static int infoFreq = 20;
    private static int infoCnt = 0;

    public static void writeMemoryInfo()
    {
        if(infoCnt == 0)
        {
            final int totalMem = (int)(Runtime.getRuntime().totalMemory() / 1024);
            final int freeMem = (int)(Runtime.getRuntime().freeMemory() / 1024);
            write("[Memory info] " + freeMem + " kB / " + totalMem + " kB\n");
        }

        infoCnt++;
        if(infoCnt >= infoFreq)
            infoCnt = 0;
    }
}
