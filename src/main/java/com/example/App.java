package com.example;

import com.opencsv.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class App {

    static class Division {
        private static int counter = 1;
        int id;
        String name;

        public Division(String name) {
            this.id = counter++;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }

    static class Person {
        int id;
        String name;
        String floor;
        Division division;
        double salary;
        Date dateOfBirth;

        public Person(int id, String name, String floor, Division division, double salary, Date dateOfBirth) {
            this.id = id;
            this.name = name;
            this.floor = floor;
            this.division = division;
            this.salary = salary;
            this.dateOfBirth = dateOfBirth;
        }

        @Override
        public String toString() {
            return id + ", " + name + ", " + floor + ", " + division.name + ", " + salary + ", " + new SimpleDateFormat("yyyy-MM-dd").format(dateOfBirth);
        }
    }

    public static List<Person> readCSV(String csvFilePath, char separator) throws Exception {
        List<Person> people = new ArrayList<>();
        Map<String, Division> divisionMap = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        InputStream in = App.class.getClassLoader().getResourceAsStream(csvFilePath);
        CSVReader reader = in == null ? null :
                new CSVReaderBuilder(new InputStreamReader(in))
                        .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                        .build();

        if (reader == null) {
            throw new IllegalArgumentException("File not found: " + csvFilePath);
        }

        String[] nextLine;
        boolean isHeader = true;

        while ((nextLine = reader.readNext()) != null) {
            if (isHeader) {
                isHeader = false;
                continue;
            }

            try {
                int id = Integer.parseInt(nextLine[0]);
                String name = nextLine[1];
                String floor = nextLine[2];
                String divisionName = nextLine[3];
                double salary = Double.parseDouble(nextLine[4]);
                Date dateOfBirth = dateFormat.parse(nextLine[5]);

                Division division = divisionMap.computeIfAbsent(divisionName, Division::new);
                people.add(new Person(id, name, floor, division, salary, dateOfBirth));
            } catch (Exception e) {
                System.out.println("⚠️ Skipping invalid row: " + Arrays.toString(nextLine));
            }
        }

        return people;
    }

    public static void writeCleanedCSV(List<Person> people, File outputFile) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{"ID", "Name", "Floor", "Division", "Salary", "DateOfBirth"});
            for (Person p : people) {
                writer.writeNext(new String[]{
                        String.valueOf(p.id),
                        p.name,
                        p.floor,
                        p.division.name,
                        String.valueOf(p.salary),
                        new SimpleDateFormat("yyyy-MM-dd").format(p.dateOfBirth)
                });
            }
        }
        System.out.println("✅ Cleaned data exported to: " + outputFile.getAbsolutePath());

        // Open file automatically
        Desktop.getDesktop().open(outputFile);
    }

    public static void groupAndSummarizeAndChart(List<Person> people) {
        Map<String, List<Person>> divisionGroups = new HashMap<>();
        Map<String, Double> avgSalaries = new LinkedHashMap<>();

        for (Person p : people) {
            divisionGroups.computeIfAbsent(p.division.name, k -> new ArrayList<>()).add(p);
        }

        System.out.println("\n📊 Average Salary by Division:");
        for (String division : divisionGroups.keySet()) {
            List<Person> group = divisionGroups.get(division);
            double avg = group.stream().mapToDouble(p -> p.salary).average().orElse(0);
            avgSalaries.put(division, avg);
            System.out.printf("  %s: %.2f (%d people)%n", division, avg, group.size());
        }

        // Draw bar chart
        SwingUtilities.invokeLater(() -> createChart(avgSalaries));
    }

    public static void createChart(Map<String, Double> data) {
        JFrame frame = new JFrame("Average Salary by Division");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = getWidth();
                int height = getHeight();
                int barWidth = width / data.size();
                int maxBarHeight = height - 100;

                double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);

                int x = 10;
                for (Map.Entry<String, Double> entry : data.entrySet()) {
                    String division = entry.getKey();
                    double value = entry.getValue();

                    int barHeight = (int) ((value / maxVal) * maxBarHeight);
                    g.setColor(new Color(100, 150, 255));
                    g.fillRect(x, height - barHeight - 40, barWidth - 20, barHeight);

                    g.setColor(Color.BLACK);
                    g.drawString(division, x, height - 20);
                    g.drawString(String.format("%.0f", value), x, height - barHeight - 50);
                    x += barWidth;
                }
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            List<Person> people = readCSV("foreign_names.csv", ';');

            // ✅ Sort by salary descending then DOB ascending
            people.sort(Comparator.comparingDouble((Person p) -> -p.salary).thenComparing(p -> p.dateOfBirth));

            // ✅ Write cleaned CSV into resources folder
            File outFile = new File("src/main/resources/cleaned_people.csv");
            writeCleanedCSV(people, outFile);

            // ✅ Group & summarize + chart
            groupAndSummarizeAndChart(people);

            // ✅ Print sorted people
            System.out.println("\n📈 Sorted People (Salary ↓, DOB ↑):");
            for (Person p : people) {
                System.out.println(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
