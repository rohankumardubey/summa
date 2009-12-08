/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermEntry;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.util.Random;

/**
 * Class for testing the performance of term stat lookups.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatPerformance {
//    private static Log log = LogFactory.getLog(TermStatPerformance.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(
                    "Usage: TermStatTest [-c terms] lookups data");
            System.out.println("-c:     Create a new stats file");
            System.out.println("terms:  The number of terms to generate");
            System.out.println("lookups:The number of lookups to perform");
            System.out.println("data:   the location of the stats file");
            return;
        }
        boolean createNew = "-c".equals(args[0]);
        int offset = createNew ? 2 : 0;
        int lookups = Integer.parseInt(args[offset]);
        File data = new File(args[offset + 1]);
        if (createNew) {
            createTS(Integer.parseInt(args[1]), data).close();
        } else {
            System.out.println("Using existing term stats from '" + data + "'");
        }
        testPerformance(data, lookups);
    }

    public static void testPerformance(File location, int runs)
                                                              throws Exception {
        System.out.println(String.format(
                "testPerformance('%s', %d runs) called",location, runs));
        TermStat ts = new TermStat(Configuration.newMemoryBased());
        ts.open(location);
        int terms = (int)ts.getTermCount(); // TODO: Remove this limit
        int digits = Integer.toString(terms).length();

        Random random = new Random(87);
        Profiler profiler = new Profiler(runs);
        profiler.setBpsSpan(10000);
        int feedback = Math.max(1, runs / 100);
        for (int i = 0 ; i < runs ; i++) {
            int test = random.nextInt(terms);
            //noinspection DuplicateStringLiteralInspection
            String term = "term_" + leader(test, digits);
            ts.getTermCount(term);
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println(String.format(
                        "Executed %d/%d lookups. Average lookups/second: %d. "
                        + "ETA: %s",
                        i, runs, (int)profiler.getBps(true),
                        profiler.getETAAsString(true)));
            }
        }
        System.out.println(String.format(
                "Executed %d lookups in %s. Average lookups/second: %s",
                runs, profiler.getSpendTime(), profiler.getBps(false)));
        ts.close();
    }

    public static TermStat createTS(int terms, File location) throws Exception {
        System.out.println(
                String.format("Creating sample term stats with %d terms",
                              terms));
        int digits = Integer.toString(terms).length();
        Profiler profiler = new Profiler();
        TermStat ts = new TermStat(Configuration.newMemoryBased());
        ts.create(location);
        for (int i = 0 ; i < terms ; i++) {
            //noinspection DuplicateStringLiteralInspection
            ts.add(new TermEntry("term_" + leader(i, digits), i + 2));
        }
        ts.store();
        ts.setDocCount(terms);
        ts.setSource("TermStatPerformance test data for " + terms + " terms");
        ts.reset();
        System.out.println("Finished creating sample term stats with " + terms
                           + " terms in " + profiler.getSpendTime());
        return ts;
    }

    private static String leader(int num, int digits) {
        String result = Integer.toString(num);
        if (result.length() > digits) {
            throw new IllegalArgumentException(String.format(
                    "The number %d already has more than %d digits",
                    num, digits));
        }
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }
}
