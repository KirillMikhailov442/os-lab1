import java.io.*;

public class SysInfoLinux {

    public static void main(String[] args) {
        String os = runCommand("lsb_release -d");
        if (os != null) {
            int colonIndex = os.indexOf(':');
            if (colonIndex != -1) {
                os = os.substring(colonIndex + 1).trim();
            }
        } else {
            os = readFileLine("/etc/os-release", "PRETTY_NAME=");
        }

        // OS name
        System.out.println("OS: " + os);

        // Kernel
        System.out.println("Kernel: Linux " + runCommand("uname -r"));

        // Hostname
        System.out.println("Hostname: " + runCommand("hostname"));

        // User
        System.out.println("User: " + System.getProperty("user.name"));

        // RAM
        String meminfo = readFile("/proc/meminfo");
        System.out.printf("RAM: %dMB free / %dMB total%n",
                extractValue(meminfo, "MemFree:"), extractValue(meminfo, "MemTotal:"));
        System.out.printf("Swap: %dMB free / %dMB total%n",
                extractValue(meminfo, "SwapFree:"), extractValue(meminfo, "SwapTotal:"));
        System.out.printf("Virtual memory: %d MB%n", extractValue(meminfo, "VmallocTotal:"));

        // Processors
        System.out.println("Processors: " + runCommand("nproc"));

        // Load average
        String[] loads = runCommand("cat /proc/loadavg").split("\\s+");
        System.out.printf("Load average: %.2f, %.2f, %.2f%n",
                Double.parseDouble(loads[0]),
                Double.parseDouble(loads[1]),
                Double.parseDouble(loads[2]));

        // Disks
        String dfOutput = runCommand("df -h --output=target,fstype,avail,size");
        if (dfOutput != null) {
            String[] lines = dfOutput.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].trim().split("\\s+");
                if (parts.length < 4)
                    continue;

                String mount = parts[0];
                String type = parts[1];
                String free = parts[2];
                String total = parts[3];

                if (type.equals("tmpfs") || type.equals("overlay") || type.equals("proc") || type.equals("sysfs"))
                    continue;

                System.out.printf("  %-10s %-6s %s free / %s total%n", mount, type, free, total);
            }
        }
    }

    /**
     * Запускает команды с помощью bash
     */
    private static String runCommand(String command) {
        try {
            Process procces = new ProcessBuilder("bash", "-c", command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(procces.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append("\n");
            procces.waitFor();
            return builder.toString().trim();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Читает весь файл
     */
    private static String readFile(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append("\n");
            reader.close();
            return builder.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Читает файл и ищет строку
     */
    private static String readFileLine(String path, String prefix) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(prefix))
                    return line.substring(prefix.length()).replace("\"", "").trim();
            }
        } catch (IOException ignored) {
        }
        return "Unknown OS";
    }

    /**
     * Извлекает числовое значение из строки
     */
    private static long extractValue(String content, String prefix) {
        for (String line : content.split("\n")) {
            if (line.startsWith(prefix))
                return Long.parseLong(line.split("\\s+")[1]) / 1024;
        }
        return 0;
    }
}