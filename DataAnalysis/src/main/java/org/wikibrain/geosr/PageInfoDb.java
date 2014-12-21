package org.wikibrain.geosr;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PageInfoDb {
    private static final Logger LOG = Logger.getLogger(PageInfoDb.class.getCanonicalName());

    private Map<String, PageInfo> byTitle = new HashMap<String, PageInfo>();
    private Map<Integer, PageInfo> byId = new HashMap<Integer, PageInfo>();

    public PageInfoDb() throws FileNotFoundException {
        readPopularity();
        readScales();
    }

    private void readPopularity() throws FileNotFoundException {
        File file = new File("dat/pagePopularity.txt");
        Scanner scanner = new Scanner(file);
        int rank = 0;
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            StringTokenizer st= new StringTokenizer(next,"\t",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            PageInfo pi = new PageInfo();
            pi.id = id;
            pi.title = name;
            pi.viewRank = rank++;
            byId.put(id, pi);
            byTitle.put(name, pi);
        }
        scanner.close();
        LOG.info("read " + byId.size() + " pages");
    }

    private void readScales() throws FileNotFoundException {
        Scanner scanner= new Scanner(new File("dat/pageScales.txt"));

        //Throw out the first 7 lines because they are information
        for (int i = 0; i <7 ; i++) {
            scanner.nextLine();
        }

        int numMatches = 0;
        while(scanner.hasNextLine()){
            String s= scanner.nextLine();
            String[] info= s.split("\t", -1);
            int id = Integer.parseInt(info[0]);
            int scale = Integer.parseInt(info[1]);
            if (byId.containsKey(id)) {
                byId.get(id).scale = scale;
                numMatches++;
            }
        }
        LOG.info("added scale to " + numMatches + " pages");
    }

    public static void main(String args[]) throws FileNotFoundException {
        PageInfoDb db = new PageInfoDb();
    }
}
