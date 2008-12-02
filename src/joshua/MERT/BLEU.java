package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class BLEU extends EvaluationMetric
{
  protected int maxGramLength;
  protected int effLengthMethod;
    // 1: closest, 2: shortest, 3: average
//  protected HashMap[][] maxNgramCounts;
  protected HashMap<String,Integer>[] maxNgramCounts;
  protected int[][] refWordCount;
  protected double[] weights;

  public BLEU()
  {
    maxGramLength = 4; // default
    effLengthMethod = 1; // default
    initialize();
  }

  public BLEU(int mxGrmLn)
  {
    if (mxGrmLn >= 1) {
      maxGramLength = mxGrmLn;
    } else {
      System.out.println("Maximum gram length must be positive");
      System.exit(2);
    }
    effLengthMethod = 1; // default
    initialize();
  }

  public BLEU(String methodStr)
  {
    maxGramLength = 4; // default

    if (methodStr.equals("closest")) {
      effLengthMethod = 1;
    } else if (methodStr.equals("shortest")) {
      effLengthMethod = 2;
    } else if (methodStr.equals("average")) {
      effLengthMethod = 3;
    } else {
      System.out.println("Unknown effective length method string " + methodStr + ".");
      System.out.println("Should be one of closest, shortest, or average.");
      System.exit(1);
    }

    initialize();
  }

  public BLEU(int mxGrmLn,String methodStr)
  {
    if (mxGrmLn >= 1) {
      maxGramLength = mxGrmLn;
    } else {
      System.out.println("Maximum gram length must be positive");
      System.exit(2);
    }

    if (methodStr.equals("closest")) {
      effLengthMethod = 1;
    } else if (methodStr.equals("shortest")) {
      effLengthMethod = 2;
    } else if (methodStr.equals("average")) {
      effLengthMethod = 3;
    } else {
      System.out.println("Unknown effective length method string " + methodStr + ".");
      System.out.println("Should be one of closest, shortest, or average.");
      System.exit(1);
    }

    initialize();

  }



  protected void initialize()
  {
    metricName = "BLEU";
    toBeMinimized = false;
    set_suffStatsCount();
    set_weightsArray();
    set_maxNgramCounts();
  }

  public double bestPossibleScore() { return 1.0; }
  public double worstPossibleScore() { return 0.0; }

  protected void set_suffStatsCount()
  {
    suffStatsCount = 1 + maxGramLength + 2;
      // 1 for number of segments, 1 per gram length for its precision, and 2 for length info
  }

  protected void set_weightsArray()
  {
    weights = new double[1+maxGramLength];
    for (int n = 1; n <= maxGramLength; ++n) {
      weights[n] = 1.0/maxGramLength;
    }
  }

  protected void set_maxNgramCounts()
  {
//    maxNgramCounts = new HashMap[numSentences][1+maxGramLength];
    maxNgramCounts = new HashMap[numSentences];
    String gram = "";
    int oldCount = 0, nextCount = 0;

//    for (int n = 1; n <= maxGramLength; ++n) {
      for (int i = 0; i < numSentences; ++i) {
//        maxNgramCounts[i][n] = getNgramCountsArray(refSentences[i][0])[n];
        maxNgramCounts[i] = getNgramCountsAll(refSentences[i][0]);
          // initialize to ngramCounts[n] of the first reference translation...

        // ...and update as necessary from the other reference translations
        for (int r = 1; r < refsPerSen; ++r) {
//          HashMap<String,Integer> nextNgramCounts = getNgramCountsArray(refSentences[i][r])[n];
          HashMap<String,Integer> nextNgramCounts = getNgramCountsAll(refSentences[i][r]);
          Iterator<String> it = (nextNgramCounts.keySet()).iterator();

          while (it.hasNext()) {
            gram = it.next();
            nextCount = nextNgramCounts.get(gram);

//            if (maxNgramCounts[i][n].containsKey(gram)) { // update if necessary
//              oldCount = maxNgramCounts[i][n].get(gram);
            if (maxNgramCounts[i].containsKey(gram)) { // update if necessary
              oldCount = maxNgramCounts[i].get(gram);
              if (nextCount > oldCount) {
//                maxNgramCounts[i][n].put(gram,nextCount);
                maxNgramCounts[i].put(gram,nextCount);
              }
            } else { // add it
//              maxNgramCounts[i][n].put(gram,nextCount);
              maxNgramCounts[i].put(gram,nextCount);
            }

          }

        } // for (r)

      } // for (i)

//    } // for (n)

    // Reference sentences are not needed anymore, since the gram counts are stored.
    // The only thing we need are their lenghts, to be used in effLength, so store
    // the lengths before discarding the reference sentences...

    refWordCount = new int[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        refWordCount[i][r] = wordCount(refSentences[i][r]);
        refSentences[i][r] = null;
      }
      refSentences[i] = null;
    }

    refSentences = null;

  }




  public int[] suffStats(String cand_str, int i)
  {
    int[] stats = new int[suffStatsCount];
    stats[0] = 1;

    String[] words = cand_str.split("\\s+");

int wordCount = words.length;
for (int j = 0; j < wordCount; ++j) { words[j] = words[j].intern(); }

    set_prec_suffStats(stats,words,i);

    stats[maxGramLength+1] = words.length;
    stats[maxGramLength+2] = effLength(words.length,i);

    return stats;
  }

  public void set_prec_suffStats(int[] stats, String[] words, int i)
  {
    HashMap[] candCountsArray = getNgramCountsArray(words);

    for (int n = 1; n <= maxGramLength; ++n) {

      int correctGramCount = 0;
      String gram = "";
      int candGramCount = 0, maxRefGramCount = 0, clippedCount = 0;

      Iterator it = (candCountsArray[n].keySet()).iterator();

      while (it.hasNext()) {
      // for each gram type in the candidate
        gram = (String)it.next();
        candGramCount = (Integer)candCountsArray[n].get(gram);
//        if (maxNgramCounts[i][n].containsKey(gram)) {
//          maxRefGramCount = maxNgramCounts[i][n].get(gram);
        if (maxNgramCounts[i].containsKey(gram)) {
          maxRefGramCount = maxNgramCounts[i].get(gram);
        } else {
          maxRefGramCount = 0;
        }

        clippedCount = Math.min(candGramCount,maxRefGramCount);

//        clippedCount = (candGramCount < maxRefGramCount ? candGramCount : maxRefGramCount);

        correctGramCount += clippedCount;

      }

      stats[n] = correctGramCount;

    }
  }

  public int effLength(int candLength, int i)
  {
    if (effLengthMethod == 1) { // closest

      int closestRefLength = refWordCount[i][0];
      int minDiff = Math.abs(candLength-closestRefLength);

//int minDiff = candLength-closestRefLength;
//if (minDiff < 0) minDiff = -minDiff;

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refWordCount[i][r];
        int nextDiff = Math.abs(candLength-nextRefLength);

//int nextDiff = candLength-nextRefLength;
//if (nextDiff < 0) nextDiff = -nextDiff;

        if (nextDiff < minDiff) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        } else if (nextDiff == minDiff && nextRefLength < closestRefLength) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        }
      }

      return closestRefLength;

    } else if (effLengthMethod == 2) { // shortest

      int shortestRefLength = refWordCount[i][0];

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refWordCount[i][r];
        if (nextRefLength < shortestRefLength) {
          shortestRefLength = nextRefLength;
        }
      }

      return shortestRefLength;

    }
/* // commented out because it needs sufficient statistics to be doubles
else { // average

      int totalRefLength = refWordCount[i][0];

      for (int r = 1; r < refsPerSen; ++r) {
        totalRefLength += refWordCount[i][r];
      }

      return totalRefLength/(double)refsPerSen;

    }
*/
    return candLength; // should never get here anyway

  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(2);
    }

    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];
    double numSegments = stats[0];

    double correctGramCount, totalGramCount;

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[n];
      totalGramCount = c_len-((n-1)*numSegments);

      double prec_n;
      if (totalGramCount > 0) {
        prec_n = correctGramCount/totalGramCount;
      } else {
        prec_n = 1; // following bleu-1.04.pl ???????
      }

      if (prec_n == 0) {
        smooth_addition *= 0.5;
        prec_n = smooth_addition / (c_len-n+1);
        // isn't c_len-n+1 just totalGramCount ???????
      }

      BLEUsum += weights[n] * Math.log(prec_n);

    }

    double BP = 1.0;
    if (c_len < r_len) BP = Math.exp(1-(r_len/c_len));
      // if c_len > r_len, no penalty applies

    return BP*Math.exp(BLEUsum);

  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];
    double numSegments = stats[0];

    double correctGramCount, totalGramCount;

    if (oneLiner) {
      System.out.print("Precisions: ");
    }

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[n];
      totalGramCount = c_len-((n-1)*numSegments);
      double prec_n = correctGramCount/totalGramCount;
        // what if totalGramCount is zero ???????????????????????????????

      if (prec_n > 0) {
        if (oneLiner) {
          System.out.print(n + "=" + f4.format(prec_n) + ", ");
        } else {
          System.out.println("BLEU_precision(" + n + ") = " + (int)correctGramCount + " / " + (int)totalGramCount + " = " + f4.format(prec_n));
        }
      } else {
        smooth_addition *= 0.5;
        prec_n = smooth_addition / (c_len-n+1);

        if (oneLiner) {
          System.out.print(n + "~" + f4.format(prec_n) + ", ");
        } else {
          System.out.println("BLEU_precision(" + n + ") = " + (int)correctGramCount + " / " + (int)totalGramCount + " ==smoothed==> " + f4.format(prec_n));
        }
      }

      BLEUsum += weights[n] * Math.log(prec_n);

    }

    if (oneLiner) {
      System.out.print("(overall=" + f4.format(Math.exp(BLEUsum)) + "), ");
    } else {
      System.out.println("BLEU_precision = " + f4.format(Math.exp(BLEUsum)));
      System.out.println("");
    }

    double BP = 1.0;
    if (c_len < r_len) BP = Math.exp(1-(r_len/c_len));
      // if c_len > r_len, no penalty applies

    if (oneLiner) {
      System.out.print("BP=" + f4.format(BP) + ", ");
    } else {
      System.out.println("Length of candidate corpus = " + (int)c_len);
      System.out.println("Effective length of reference corpus = " + (int)r_len);
      System.out.println("BLEU_BP = " + f4.format(BP));
      System.out.println("");
    }

    System.out.println("BLEU = " + f4.format(BP*Math.exp(BLEUsum)));
  }
/*
  protected int myMin(int x, int y)
  {
    return Math.min(x,y);
//    return (x < y ? x : y);
  }

  protected int myAbs(int x)
  {
    return Math.abs(x);
//    return (x > 0 ? x : -x);
  }
*/

/*
  public int prec_suffStats(int gramLength, String[] words, int i)
  {
    int correctGramCount = 0;
    String gram = "";
    int candGramCount = 0, maxRefGramCount = 0, clippedCount = 0;

    HashMap candCounts = getNgramCounts(words,gramLength);

    Iterator it = (candCounts.keySet()).iterator();

    while (it.hasNext()) {
    // for each gram type in the candidate
      gram = (String)it.next();
      candGramCount = (Integer)candCounts.get(gram);
      if (maxNgramCounts[i][gramLength].containsKey(gram)) {
        maxRefGramCount = (Integer)maxNgramCounts[i][gramLength].get(gram);
      } else {
        maxRefGramCount = 0;
      }

      clippedCount = Math.min(candGramCount,maxRefGramCount);

//      clippedCount = (candGramCount < maxRefGramCount ? candGramCount : maxRefGramCount);

      correctGramCount += clippedCount;

    }

    return correctGramCount;

  }

  public HashMap<String,Integer> getNgramCounts(String cand_str, int n)
  {
    return getNgramCounts(cand_str.split("\\s+"),n);
  }

  public HashMap<String,Integer> getNgramCounts(String[] words, int n)
  {
    HashMap<String,Integer> ngramCounts = new HashMap<String,Integer>();
    int wordCount = words.length;

    if (wordCount >= n) {
      if (n > 1) { // for n == 1, less processing is needed
        // build the first n-gram
        int start = 0; int end = n-1;
        String gram = "";
        for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
        gram = gram + words[end];
        ngramCounts.put(gram,1);

        for (start = 1; start <= wordCount-n; ++start) {
        // process n-gram starting at start and ending at start+(n-1)

          end = start + (n-1);
          // build the n-gram from words[start] to words[end]

//// old way of doing it
//          gram = "";
//          for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
//          gram = gram + words[end];

          gram = gram.substring(gram.indexOf(' ')+1) + " " + words[end];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        } // for (start)

      } else { // if (n == 1)

        String gram = "";
        for (int j = 0; j < wordCount; ++j) {
          gram = words[j];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        }
      }
    } // if (wordCount >= n)

    return ngramCounts;
  }
*/

  protected int wordCount(String cand_str)
  {
    return cand_str.split("\\s+").length;
  }





  public HashMap<String,Integer>[] getNgramCountsArray(String cand_str)
  {
    return getNgramCountsArray(cand_str.split("\\s+"));
  }

  public HashMap<String,Integer>[] getNgramCountsArray(String[] words)
  {
    HashMap<String,Integer>[] ngramCountsArray = new HashMap[1+maxGramLength];
    ngramCountsArray[0] = null;
    for (int n = 1; n <= maxGramLength; ++n) {
      ngramCountsArray[n] = new HashMap<String,Integer>();
    }

    int len = words.length;
    String gram;
    int st = 0;

    for (; st <= len-maxGramLength; ++st) {

      gram = words[st];
      if (ngramCountsArray[1].containsKey(gram)) {
        int oldCount = ngramCountsArray[1].get(gram);
        ngramCountsArray[1].put(gram,oldCount+1);
      } else {
        ngramCountsArray[1].put(gram,1);
      }

      for (int n = 2; n <= maxGramLength; ++n) {
        gram = gram + " " + words[st+n-1];
        if (ngramCountsArray[n].containsKey(gram)) {
          int oldCount = ngramCountsArray[n].get(gram);
          ngramCountsArray[n].put(gram,oldCount+1);
        } else {
          ngramCountsArray[n].put(gram,1);
        }
      } // for (n)

    } // for (st)

    // now st is either len-maxGramLength+1 or zero (if above loop never entered, which
    // happens with sentences that have fewer than maxGramLength words)

    for (; st < len; ++st) {

      gram = words[st];
      if (ngramCountsArray[1].containsKey(gram)) {
        int oldCount = ngramCountsArray[1].get(gram);
        ngramCountsArray[1].put(gram,oldCount+1);
      } else {
        ngramCountsArray[1].put(gram,1);
      }

      int n = 2;
      for (int fin = st+1; fin < len; ++fin) {
        gram = gram + " " + words[st+n-1];

        if (ngramCountsArray[n].containsKey(gram)) {
          int oldCount = ngramCountsArray[n].get(gram);
          ngramCountsArray[n].put(gram,oldCount+1);
        } else {
          ngramCountsArray[n].put(gram,1);
        }
        ++n;
      } // for (fin)

    } // for (st)

    return ngramCountsArray;

  }




  public HashMap<String,Integer> getNgramCountsAll(String cand_str)
  {
    return getNgramCountsAll(cand_str.split("\\s+"));
  }

  public HashMap<String,Integer> getNgramCountsAll(String[] words)
  {
    HashMap<String,Integer> ngramCountsAll = new HashMap<String,Integer>();

    int len = words.length;
    String gram;
    int st = 0;

    for (; st <= len-maxGramLength; ++st) {

      gram = words[st];
      if (ngramCountsAll.containsKey(gram)) {
        int oldCount = ngramCountsAll.get(gram);
        ngramCountsAll.put(gram,oldCount+1);
      } else {
        ngramCountsAll.put(gram,1);
      }

      for (int n = 2; n <= maxGramLength; ++n) {
        gram = gram + " " + words[st+n-1];
        if (ngramCountsAll.containsKey(gram)) {
          int oldCount = ngramCountsAll.get(gram);
          ngramCountsAll.put(gram,oldCount+1);
        } else {
          ngramCountsAll.put(gram,1);
        }
      } // for (n)

    } // for (st)

    // now st is either len-maxGramLength+1 or zero (if above loop never entered, which
    // happens with sentences that have fewer than maxGramLength words)

    for (; st < len; ++st) {

      gram = words[st];
      if (ngramCountsAll.containsKey(gram)) {
        int oldCount = ngramCountsAll.get(gram);
        ngramCountsAll.put(gram,oldCount+1);
      } else {
        ngramCountsAll.put(gram,1);
      }

      int n = 2;
      for (int fin = st+1; fin < len; ++fin) {
        gram = gram + " " + words[st+n-1];

        if (ngramCountsAll.containsKey(gram)) {
          int oldCount = ngramCountsAll.get(gram);
          ngramCountsAll.put(gram,oldCount+1);
        } else {
          ngramCountsAll.put(gram,1);
        }
        ++n;
      } // for (fin)

    } // for (st)

    return ngramCountsAll;

  }



/*
  // The following two functions are nice to have, I suppose, but they're never
  // used, so they're commented out at the moment for clarity's sake
  public int prec_suffStats(int gramLength, String[] topCand_str)
  {
    int totCount = 0;

    for (int i = 0; i < numSentences; ++i) {
      String[] words = topCand_str[i].split("\\s+");
      totCount += prec_suffStats(gramLength,words,i);
    } // for (i)

    return totCount;
  }

  public int effLength(String[] topCand_str)
  {
    int totLength = 0;

    for (int i = 0; i < numSentences; ++i) {
      String[] words = topCand_str[i].split("\\s+");
      totLength += effLength(words.length,i);
    } // for (i)

    return totLength;
  }
*/


}
