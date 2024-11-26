package stdhuff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String data = "", processedData = "";
        Scanner consoleScanner = new Scanner(System.in);

        System.out.println("1-Compress\n2-Decompress");
        if (consoleScanner.nextInt() == 1) {
            // Read data from file
            System.out.print("Input file: ");
            File inFile = new File(consoleScanner.next() + ".txt");
            try (Scanner fileScanner = new Scanner(inFile)) {
                while (fileScanner.hasNextLine())
                    data += fileScanner.nextLine();
            } catch (IOException e) {
                System.out.println("Couldn't read from file: " + e.getMessage());
                System.exit(1);
            }

            // Construct the tree
            Map<Character, Float> probabilityMap = new HashMap<>();
            System.out.println("1-Read probabilities from a file");
            System.out.println("2-Calculate probabilities from previously input file");
            if (consoleScanner.nextInt() == 1) {
                System.out.print("Enter name of the file with probabilities: ");
                File probabilitiesFile = new File(consoleScanner.next() + ".txt");
                try (Scanner probabilitiesFileScanner = new Scanner(probabilitiesFile)) {
                    while (probabilitiesFileScanner.hasNextLine()) {
                        String[] line = probabilitiesFileScanner.nextLine().split(" ");
                        probabilityMap.put(line[0].charAt(0), Float.parseFloat(line[1]));
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                for (char c : data.toCharArray()) {
                    probabilityMap.merge(c, 1.0f, Float::sum);
                }
                int dataLen = data.length();
                probabilityMap.replaceAll((key, value) -> (value / dataLen));
            }

            ArrayList<Node> nodes = new ArrayList<>();
            probabilityMap.forEach((key, value) -> {
                Node node = new Node();
                node.character = key;
                node.probability = value;
                node.isLeaf = true;
                nodes.add(node);
            });
            nodes.sort((n1, n2) -> Float.compare(n1.probability, n2.probability));

            while (nodes.size() != 1) {
                Node first = nodes.get(0);
                Node second = nodes.get(1);
                nodes.remove(first);
                nodes.remove(second);
                Node newNode = new Node();
                newNode.probability = first.probability + second.probability;
                newNode.isLeaf = false;
                newNode.children[0] = second;
                newNode.children[1] = first;
                int i;
                for (i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).probability >= newNode.probability)
                        break;
                }
                nodes.add(i, newNode);
            }

            Map<Character, String> alphaToBinary = new HashMap<>();
            recursiveAdd(nodes.get(0), alphaToBinary, "");
            if (alphaToBinary.size() == 1)
                alphaToBinary.replaceAll((key, value) -> "0");

            
                for (char c : data.toCharArray()) {
                    processedData += alphaToBinary.get(c);
                }
                System.out.print("Output probabilities to a file? (1-yes, 2-no): ");
                if (consoleScanner.nextInt() == 1) {
                    String probabilities = "";
                    for (Map.Entry<Character, Float> entry : probabilityMap.entrySet()) {
                        probabilities += entry.getKey() + " " + entry.getValue() + "\n";
                    }
                    System.out.print("Probability output file: ");
                    try (FileWriter writer = new FileWriter(consoleScanner.next() + ".txt")) {
                        writer.write(probabilities);
                    } catch (IOException e) {
                        System.out.println("Couldn't write to file: " + e.getMessage());
                    }
                }

                // write table to txt
                String alphaToBinaryString = "";
                for (Map.Entry<Character, String> entry : alphaToBinary.entrySet()) {
                    alphaToBinaryString += entry.getKey() + " " + entry.getValue() + '\n';
                }
                System.out.print("Output alpha-to-binary table file: ");
                try (FileWriter writer = new FileWriter(consoleScanner.next() + ".txt")) {
                    writer.write(alphaToBinaryString);
                } catch (IOException e) {
                    System.out.println("Couldn't write to file: " + e.getMessage());
                }

                // write compressed bits to the bin file
                System.out.print("Compressed binary file name: ");
                String binaryFileName = consoleScanner.next() + ".bin";
                try (FileOutputStream binaryWriter = new FileOutputStream(binaryFileName)) {
                // calc padding len
                int paddingLength = 8 - (processedData.length() % 8);
                if (paddingLength == 8) paddingLength = 0; // if size is a multiple of 8 -> no padding

                // save padding len in first byte
                binaryWriter.write(paddingLength);
                // do the padding for as many required to be a multiple of 8
                while (processedData.length() % 8 != 0) {
                    processedData += "0";
                }

                // write data in bin file
                for (int i = 0; i < processedData.length(); i += 8) {
                    String byteString = processedData.substring(i, i + 8);
                    int byteValue = Integer.parseInt(byteString, 2);
                    binaryWriter.write(byteValue);
                }

                System.out.println("Compressed binary data saved to " + binaryFileName);

            } catch (IOException e) {
                System.out.println("Couldn't write to binary file: " + e.getMessage());
            }

        } else {
            System.out.print("Codes file name: ");
            String codesFileName = consoleScanner.next() + ".txt";
            System.out.print("Compressed binary file name: ");
            String binaryFileName = consoleScanner.next() + ".bin";

            Map<Character, String> codesTable = new HashMap<>();
            
            try (Scanner codesFileScanner = new Scanner(new File(codesFileName))) {
                while (codesFileScanner.hasNextLine()) {
                    String line = codesFileScanner.nextLine();
                    codesTable.put(line.charAt(0), line.substring(2));
                }
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find codes file: " + e.getMessage());
            }

            // reading from binary file
            String compressedString = "";
            try (FileInputStream binaryReader = new FileInputStream(binaryFileName)) {
                int byteValue;
                // read padding length (first byte in binary file)
                int paddingLength = binaryReader.read(); // First byte is the padding length

                // read rest of compressed data from the bin file
                while ((byteValue = binaryReader.read()) != -1) {
                    String byteString = String.format("%8s", Integer.toBinaryString(byteValue & 0xFF)).replace(' ', '0');
                    compressedString += byteString;
                }
                System.out.println("Compressed binary data read successfully.");

                // remove extra bits used for padding
                compressedString = compressedString.substring(0, compressedString.length() - paddingLength);

            } catch (IOException e) {
                System.out.println("Couldn't read from binary file: " + e.getMessage());
            }

            String searchString = "";
            for (int i = 0; i < compressedString.length(); i++) {
                searchString += compressedString.charAt(i);
                for (Map.Entry<Character, String> pair : codesTable.entrySet()) {
                    if (pair.getValue().equals(searchString)) {
                        processedData += pair.getKey();
                        searchString = "";
                        break;
                    }
                }
            }

        }

        System.out.print("Output file name: ");
        try (FileWriter writer = new FileWriter(consoleScanner.next() + ".txt")) {
            writer.write(processedData);
        } catch (IOException e) {
            System.out.println("Couldn't write to file: " + e.getMessage());
        }
    }

    static void recursiveAdd(Node node, Map<Character, String> map, String binaryCode) {
        if (node.isLeaf) {
            map.put(node.character, binaryCode);
        } else {
            recursiveAdd(node.children[0], map, binaryCode + "0");
            recursiveAdd(node.children[1], map, binaryCode + "1");
        }
    }
}
