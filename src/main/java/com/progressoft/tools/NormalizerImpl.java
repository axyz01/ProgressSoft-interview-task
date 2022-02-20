package com.progressoft.tools;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NormalizerImpl implements Normalizer {

    /** Used on dividing BigDecimal etc...*/
    static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    static final MathContext MATH_CTX = new MathContext(0, ROUNDING_MODE);

    /** Available normalization formulas (for command line) */
    static String[] NORMALIZATION_METHODS = new String[] { "min-max", "z-score" };

    public NormalizerImpl() {
        // Done; Make the compiler work more easy :D
    }

    @Override
    public ScoringSummary zscore(Path csvPath, Path destPath, String colToStandardize) {

        if(!Files.exists(csvPath)) {
            throw new IllegalArgumentException("source file not found");
        }

        return getCSVTaskInstance(csvPath, destPath, colToStandardize, colToStandardize + "_z")
                .summarizeAndOutputZScoreFormula();
    }

    @Override
    public ScoringSummary minMaxScaling(Path csvPath, Path destPath, String colToNormalize) {

        if(!Files.exists(csvPath)) {
            throw new IllegalArgumentException("source file not found");
        }

        return getCSVTaskInstance(csvPath, destPath, colToNormalize, colToNormalize + "_mm")
                .summarizeAndOutputMinMaxFormula();
    }

    public static CSVTask getCSVTaskInstance(Path inputCsvPath, Path outputCsvPath, String col, String outputCol) {

        return new CSVTask(inputCsvPath.toFile(), outputCsvPath.toFile(), col, outputCol);
    }

    /**
     * Convert <code>BigDecimal</code> to the required format.
     * Which is 2 decimal places and HALF_EVEN rounding mode.
     *
     * @param number desired number to re-scale
     * @return new scaled number
     */
    public static BigDecimal convertToRequiredScale(BigDecimal number) {
        return number.setScale(2, ROUNDING_MODE);
    }


    /**
     * Normalize a number of type <code>BigDecimal</code> by Min-Max normalization formula.
     * <p>>Since the new min & max are not specified; I assumed they are (0,1) based on the provided test output.</p>
     *
     * @param number to normalize
     * @param minInList the minimum number in the group which the <code>number</code> argument parameter within
     * @param maxInList the maximum number in the group which the <code>number</code> argument parameter within
     *
     * @return normalized number
     */
    static BigDecimal normalizeMinMax(BigDecimal number, BigDecimal minInList, BigDecimal maxInList){

        /* (number - min) / (max - min) */
        BigDecimal r = number.subtract(minInList).divide(maxInList.subtract(minInList), 2, ROUNDING_MODE);

        return convertToRequiredScale(r);
    }

    /**
     * Normalize a number of type <code>BigDecimal</code> by Z-Score normalization formula.
     *
     * @param number to normalize
     * @param mean the resulted mean of the group which the <code>number</code> argument within
     * @param sd the resulted standard deviation of the group which the <code>number</code> argument within
     * @return
     */
    static BigDecimal normalizeZScore(BigDecimal number, BigDecimal mean, BigDecimal sd) {

        /* (number - mean) / sd */
        BigDecimal score = number.subtract(mean).divide(sd, 2, ROUNDING_MODE);

        return convertToRequiredScale(score);
    }

    static ScoringSummary getScoringSummary(List<BigDecimal> numbers) {
        return new ScoringSummary() {
            @Override
            public BigDecimal mean() {
                return convertToRequiredScale(BigDecimalUtils.mean(numbers));
            }

            @Override
            public BigDecimal standardDeviation() {
                return convertToRequiredScale(BigDecimalUtils.standardDeviation(numbers, MATH_CTX));
            }

            @Override
            public BigDecimal variance() {
                return convertToRequiredScale(BigDecimalUtils.populationVariance(numbers));
            }

            @Override
            public BigDecimal median() {
                return convertToRequiredScale(BigDecimalUtils.median(numbers));
            }

            @Override
            public BigDecimal min() {
                return convertToRequiredScale(BigDecimalUtils.min(numbers));
            }

            @Override
            public BigDecimal max() {
                return convertToRequiredScale(BigDecimalUtils.max(numbers));
            }
        };
    }

    /**
     * Handle the whole operations (reading, writing etc...).
     */
    public static class CSVTask {

        private final String colToNormalize;
        private final String colToWrite;

        private BufferedReader reader;
        private BufferedWriter writer;

        static final String SPLIT_BY = ",";
        static final String LINE_SEPARATOR = System.getProperty("line.separator");

        private final List<String> outputList;

        private int colToNormalizeIndex = -1;

        public CSVTask(File input, File output, String colToNormalize, String colToWrite) {

            outputList = new ArrayList<>();

            this.colToNormalize = colToNormalize;
            this.colToWrite = colToWrite;

            reader = null;
            writer = null;

            try {
                reader = new BufferedReader(new FileReader(input));
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        private String appendToTheNextOfSpecifiedCol(String origLine, String str) {

            List<String> cols = new ArrayList<>(Arrays.asList(origLine.split(",")));
            cols.add(colToNormalizeIndex + 1, str);

            return String.join(",", cols);
        }

        private ScoringSummary summarizeAndOutputMinMaxFormula() {

            handleFirstLine();

            List<BigDecimal> numbersList = listInputBySpecificCol();
            int n = numbersList.size();

            ScoringSummary summary = getScoringSummary(numbersList);

            for(int i = 0; i < n; i++) {

                BigDecimal result = convertToRequiredScale(
                        normalizeMinMax(numbersList.get(i), summary.min(), summary.max()));

                String newLine = appendToTheNextOfSpecifiedCol(outputList.get(i), result.toString());

                outputList.set(i, newLine + (i == n -1 ? "":LINE_SEPARATOR));

            }

            writeToTheOutputFile();

            return summary;
        }

        private ScoringSummary summarizeAndOutputZScoreFormula() {

            handleFirstLine();

            List<BigDecimal> numbersList = listInputBySpecificCol();
            int n = numbersList.size();

            ScoringSummary summary = getScoringSummary(numbersList);

            for(int i = 0; i < n; i++) {

                BigDecimal result = convertToRequiredScale(
                        normalizeZScore(numbersList.get(i), summary.mean(), summary.standardDeviation()));

                String newLine = appendToTheNextOfSpecifiedCol(outputList.get(i), result.toString());

                outputList.set(i, newLine + (i == n - 1 ? "":LINE_SEPARATOR));

            }

            writeToTheOutputFile();

            return summary;
        }

        /**
         * Manipulate the CSV file attributes
         */
        private void handleFirstLine() {
            try {
                String origFirstLine = reader.readLine();

                List<String> columns = Arrays.asList(origFirstLine.split(SPLIT_BY));

                colToNormalizeIndex = columns.indexOf(colToNormalize);

                if(colToNormalizeIndex == -1) {
                    throw new IllegalArgumentException(String.format("column %s not found", colToNormalize));
                }

                String newLine = appendToTheNextOfSpecifiedCol(origFirstLine, colToWrite);

                writer.write(newLine + LINE_SEPARATOR);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Read the row of the specified column from the provided CSV file
         *
         * @return list of numbers
         */
        private List<BigDecimal> listInputBySpecificCol() {

            List<BigDecimal> result = new ArrayList<>();
            String line;

            try {
                while((line = reader.readLine()) != null){
                    BigDecimal item = new BigDecimal(line.split(SPLIT_BY)[colToNormalizeIndex]);
                    result.add(item);
                    outputList.add(line);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return result;
        }

        void writeToTheOutputFile() {
            try {
                for(String line : outputList) {
                    writer.write(line);
                }
                writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    static class BigDecimalUtils {


        /**
         * Returns the min number in the numbers list.
         *
         * @param numbers the numbers to calculate the min.
         * @return the min number in the numbers list.
         */
        public static BigDecimal min(List<BigDecimal> numbers) {
            return new TreeSet<>(numbers).first();
        }

        /**
         * Returns the max number in the numbers list.
         *
         * @param numbers the numbers to calculate the max.
         * @return the max number in the numbers list.
         */
        public static BigDecimal max(List<BigDecimal> numbers) {
            return new TreeSet<>(numbers).last();
        }

        /**
         * Returns the sum number in the numbers list.
         *
         * @param numbers the numbers to calculate the sum.
         * @return the sum of the numbers.
         */
        public static BigDecimal sum(List<BigDecimal> numbers) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal number : numbers) {
                sum = sum.add(number);
            }
            return sum;
        }

        /**
         * Returns the mean number in the numbers list.
         *
         * @param numbers the numbers to calculate the mean.
         * @return the mean of the numbers.
         */
        public static BigDecimal mean(List<BigDecimal> numbers) {
            BigDecimal sum = sum(numbers);
            return sum.divide(new BigDecimal(numbers.size()), 0, ROUNDING_MODE);
        }

        public static BigDecimal median(List<BigDecimal> numbers) {
            Collections.sort(numbers);

            if (numbers.size() % 2 == 1)
                return convertToRequiredScale(numbers.get((numbers.size() + 1) / 2 - 1));
            else {
                BigDecimal lower = numbers.get(numbers.size() / 2 - 1);
                BigDecimal upper = numbers.get(numbers.size() / 2);

                BigDecimal result = (lower.add(upper)).divide(BigDecimal.valueOf(2), MATH_CTX);

                return convertToRequiredScale(result);
            }
        }


        /**
         * Returns the standard deviation of the numbers.
         * <p><code>Double.NaN</code> is returned if the numbers list is empty.</p>
         *
         * @param numbers the numbers to calculate the standard deviation.
         * @param context the MathContext
         * @return the standard deviation
         */
        public static BigDecimal standardDeviation(List<BigDecimal> numbers, MathContext context) {
            BigDecimal sd;
            int n = numbers.size();
            if (n > 0) {
                if (n > 1) {
                    sd = BigDecimal.valueOf(Math.sqrt(populationVariance(numbers).doubleValue()));
                } else {
                    sd = BigDecimal.ZERO;
                }
            } else {
                sd = BigDecimal.valueOf(Double.NaN);
            }
            return sd;
        }

        /**
         * Computes the population variance of the available numbers list.
         * <p>The "population variance" can be computed using this statistic ( sum((x_i - mean)^2) / n ).</p>
         *
         * @param numbers the numbers to calculate the variance.
         * @return the variance of the numbers.
         */
        public static BigDecimal populationVariance(List<BigDecimal> numbers) {
            int n = numbers.size();
            if (n == 0) {
                return BigDecimal.valueOf(Double.NaN);
            } else if (n == 1) {
                return BigDecimal.ZERO;
            }
            BigDecimal mean = mean(numbers);
            List<BigDecimal> squares = new ArrayList<>();
            for (BigDecimal number : numbers) {
                BigDecimal xMinMean = number.subtract(mean);
                squares.add(xMinMean.pow(2, MATH_CTX));
            }
            BigDecimal sum = sum(squares);
            return sum.divide(new BigDecimal(numbers.size()), 0, ROUNDING_MODE);

        }

    }


    /**
     * Used to allow users to make the use of the implemented solution through command line
     *
     * @param args passed arguments
     */
    public static void main(String[] args)  {
        if(args.length != 4) {
            System.out.println("Hi! Please specify the arguments as follows: " +
                    "[SOURCE_FILE_PATH] [DEST_DIRECTORY_PATH] [COLUMN_TO_NORMALIZE] [NORMALIZATION_METHOD]");
            return;
        }

        String source = args[0], dest = args[1], col = args[2], method = args[3];

        Path sourceFilePath = Paths.get(source);

        if(!Files.exists(sourceFilePath)) {
            System.out.println("Source file not found");
            return;
        }

        if(!Files.isDirectory(Paths.get(dest))) {
            System.out.println("Please specify a valid directory to process the output file");
            return;
        }

        if(!Arrays.asList(NORMALIZATION_METHODS).contains(method)) {
            System.out.println("The selected normalization method is not available.");
            outputAvailableNormalizationMethods();
            return;
        }

        String sourceFileName = sourceFilePath.toFile().getName();
        String sourceFileNameWithoutExt = sourceFileName.substring(0, sourceFileName.lastIndexOf("."));

        Path outputFilePath = Paths.get(dest,  sourceFileNameWithoutExt + "_normalized.csv");

        boolean created = false;
        try {
            created = outputFilePath.toFile().createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(!created) {
            System.out.println("Failed to create the output file.");
            return;
        }


        if(method.equals("min-max")) {
            getCSVTaskInstance(sourceFilePath, outputFilePath, col, col + "_mm")
                    .summarizeAndOutputMinMaxFormula();
        }
        if(method.equals("z-score")) {
            getCSVTaskInstance(sourceFilePath, outputFilePath, col, col + "_z")
                    .summarizeAndOutputZScoreFormula();
        }

        System.out.println("Task was completed successfully!");
    }


    static void outputAvailableNormalizationMethods() {
        System.out.println("Available normalization methods: " +
                String.join(",", NORMALIZATION_METHODS));
    }
}