/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assemblurt;


import java.util.*;
import java.io.*;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Aldwyn Cabarrubias
 */
public final class AssemBlurt {

    public Map<String, String> symbolTable;
    public Map<String, Object[]> labels;
    public Stack<Integer> stackRegister;
    public List<Object[]> referenceTable;
    public List<String> memory;
    public List<String> fileLines;
    public String consoleText;
    public UI3 parentFrame;
    public int nextIterCtr;
    
    public AssemBlurt(UI3 frame) {
        fileLines = new ArrayList();
        parentFrame = frame;
        initComponents();
    }

    public AssemBlurt(String filename) {
        fileLines = new ArrayList();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if (!sCurrentLine.trim().equals("")) {
                    fileLines.add(sCurrentLine.trim());
                }
            }
        } catch (IOException e) {
            System.out.println("No file detected.");
        }
        initComponents();
    }
    
    public void initComponents() {
        symbolTable = new HashMap<>();
        symbolTable.put("read", "11");
        symbolTable.put("disp", "12");
        symbolTable.put("pushi", "13");
        symbolTable.put("pushv", "14");
        symbolTable.put("pop", "15");
        symbolTable.put("mod", "01");
        symbolTable.put("jmp", "21");
        symbolTable.put("jl", "22");
        symbolTable.put("jg", "23");
        symbolTable.put("jeq", "24");
        symbolTable.put("add", "04");
        symbolTable.put("sub", "05");
        symbolTable.put("cmp", "06");
        symbolTable.put("begin", "02");
        symbolTable.put("end", "03");

        stackRegister = new Stack();
        referenceTable = new ArrayList();
        memory = new ArrayList();
        for (int i = 0; i < 40; i++) {
            memory.add(null);
        }
        labels = new HashMap<>();
        consoleText = "";
    }
    
    public void setContent(String content) {
        String[] lineSplit = content.split("\n");
        for (String l: lineSplit) {
            if (!l.trim().equals("")) {
                fileLines.add(l.trim());
            }
        }
    }
    
    public void getLabels() {
        for (int i = 0; i < fileLines.size(); i++) {
            String token = fileLines.get(i);
            if (token.endsWith(":")) {
                String labelKey = token.replace(":", "");
                Object[] labelValue = new Object[]{i, "3" + labels.size()};
                labels.put(labelKey, labelValue);
            }
        }
    }
    
    public void parseInstructions() {
        getLabels();
        int lineNumber = 0;
        for (String line: fileLines) {
            String[] lineSplit = line.split(" ");
            if (symbolTable.containsKey(lineSplit[0])) {
                if (lineSplit[0].equals("begin") && lineNumber != 0) {
                    embedToConsoleArea("ERR: Misplaced `begin`.");
                    break;
                }
                Object[] toAppend = new Object[] {lineSplit[0], null, symbolTable.get(lineSplit[0]), null};
                Object param = null;
                if (lineSplit.length > 1) {
                    param = lineSplit[1];
                }
                String variable = (String) param;
                if (param instanceof String && ((String)param).matches("\\d+")) {
                    variable = String.format("%2s", param);
                    variable = variable.replace(" ", "0");
                }
                int address = 0;
                if (variable != null && !variable.matches("\\d+")) {
                    if (labels.containsKey(variable)) {
                        if (symbolTable.get(lineSplit[0]).startsWith("2")) {
                            if (labels.containsKey(lineSplit[1])) {
                                address = (int) labels.get(lineSplit[1])[0];
                            } else {
                                embedToConsoleArea("ERR: No label `" + variable + "` found.");
                                break;
                            }
                        }
                        toAppend[3] = symbolTable.get(lineSplit[0]) + address;
                    } else if (!memory.subList(30, memory.size()).contains(variable)) {
                        if (symbolTable.get(lineSplit[0]).startsWith("2")) {
                            embedToConsoleArea("ERR: No label `" + variable + "` found.");
                            break;
                        } else {
                            int pointerIndex = memory.subList(30, memory.size()).indexOf(null);
                            if (pointerIndex != -1) {
                                address = pointerIndex + 30;
                            } else {
                                embedToConsoleArea("ERR: Overflow error.");
                                break;
                            }
                            if (address < 40) {
                                memory.set(address, variable);
                                toAppend[3] = symbolTable.get(lineSplit[0]) + address;
                            } else {
                                embedToConsoleArea("ERR: Overflow error.");
                                break;
                            }
                        }
                    } else {
                        int pointerIndex = memory.subList(30, memory.size()).indexOf(variable);
                        if (pointerIndex != -1) {
                            address = pointerIndex + 30;
                        } else {
                            embedToConsoleArea("ERR: Overflow error.");
                            break;
                        }
                        memory.set(address, variable);
                        toAppend[3] = symbolTable.get(lineSplit[0]) + address;
                    }
                } else if (variable != null) {
                    toAppend[3] = symbolTable.get(lineSplit[0]) + variable;
                } else {
                    toAppend[3] = symbolTable.get(lineSplit[0]) + "00";
                }
                toAppend[1] = param;
                referenceTable.add(toAppend);
            } else {
                if (!lineSplit[0].endsWith(":")) {
                    embedToConsoleArea("ERR: Unknown instructions.");
                    break;
                } else {
                    String label = lineSplit[0].replace(":", "");
                    Object labelValue = labels.get(label)[1];
                    Object[] toAppend = new Object[] {label, null, labelValue, labelValue + "00"};
                    referenceTable.add(toAppend);
                }
            }
            lineNumber++;
        }
        for (int i = 0; i < referenceTable.size(); i++) {
            memory.set(i, (String) referenceTable.get(i)[3]);
        }
    }
    
    public void executeAllMachineCodes() {
        if (!memory.get(0).startsWith("02")) {
            embedToConsoleArea("ERR: Error");
        } else {
            Thread thread = new Thread() {
                public void run() {
                    while (memory.get(nextIterCtr) != null) {
                        boolean isToContinue = selectProcess();
                        if (!isToContinue) {
                            break;
                        }
                        nextIterCtr += 1;
                        try {
                            sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AssemBlurt.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    JOptionPane.showMessageDialog(null, "EXECUTION FINISHED.");
                    embedToConsoleArea("EXECUTION FINISHED.\nPress `RESET` to continue...");
                    try {
                        write();
                    } catch (IOException ex) {
                        Logger.getLogger(AssemBlurt.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            thread.start();
        }
    }
    
    public void executeNextMachineCodes() throws IOException {
        if (!memory.get(0).startsWith("02")) {
            embedToConsoleArea("ERR: Error");
        } else {
            if (nextIterCtr < fileLines.size()) {
                selectProcess();
                nextIterCtr += 1;
            } else {
                parentFrame.getExecuteNextBtn().setEnabled(false);
                JOptionPane.showMessageDialog(null, "EXECUTION FINISHED.");
                embedToConsoleArea("EXECUTION FINISHED.\nPress `RESET` to continue...");
                write();
            }
        }
    }
    
    public boolean selectProcess() {
        embedToConsoleArea("Executing line " + nextIterCtr + ": " + fileLines.get(nextIterCtr));
        embedToMemoryArea();
        embedToStackRegArea();
        String mem = memory.get(nextIterCtr);
        int memValue = Integer.parseInt(mem.substring(2));
        if (mem.startsWith("01")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Operand Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                int mod = top2 % top1;
                if (stackRegister.size() > 5) {
                    embedToConsoleArea("ERR: Stack Overflow");
                    return false;
                } else {
                    stackRegister.push(mod);
                }
            }
        } else if (mem.startsWith("11")) {
            String valueStr = JOptionPane.showInputDialog(null, "Enter an integer");
            while (!valueStr.trim().matches("\\d+") && valueStr.trim().length() <= 2) {
                valueStr = JOptionPane.showInputDialog(null, "ERR: Invalid input. Enter an integer");
            }
            memory.set(memValue, valueStr.trim());
        } else if (mem.startsWith("14")) {
            if (stackRegister.size() > 5) {
                embedToConsoleArea("ERR: Stack Overflow");
                return false;
            } else {
                stackRegister.push(Integer.parseInt(memory.get(memValue)));
            }
        } else if (mem.startsWith("13")) {
            if (stackRegister.size() > 5) {
                embedToConsoleArea("ERR: Stack Overflow");
                return false;
            } else {
                stackRegister.push(memValue);
            }
        } else if (mem.startsWith("24")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Compare Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                if (top1 == top2) {
                    nextIterCtr = Integer.parseInt(mem.substring(2));
                }
            }
        } else if (mem.startsWith("15")) {
            if (stackRegister.isEmpty()) {
                embedToConsoleArea("ERR: Empty Stack Error");
                return false;
            } else {
                int value = stackRegister.pop();
                memory.set(memValue, Integer.toString(value));
            }
        } else if (mem.startsWith("21")) {
            nextIterCtr = memValue;
        } else if (mem.startsWith("12")) {
            embedToConsoleArea("Result: " + memory.get(memValue));
        } else if (mem.startsWith("03")) {
            return false;
        } else if (mem.startsWith("22")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Compare Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                if (top1 < top2) {
                    nextIterCtr = memValue;
                }
            }
        } else if (mem.startsWith("23")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Compare Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                if (top1 > top2) {
                    nextIterCtr = memValue;
                }
            }
        } else if (mem.startsWith("04")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Operand Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                int sum = top1 + top2;
                if (sum > 99) {
                    embedToConsoleArea("ERR: Arithmetic Overflow Error");
                    return false;
                } else {
                    if (stackRegister.size() > 5) {
                        embedToConsoleArea("ERR: Stack Overflow");
                        return false;
                    } else {
                        stackRegister.push(sum);
                    }
                }
            }
        } else if (mem.startsWith("05")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Operand Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                int diff = top1 - top2;
                if (diff < 0) {
                    embedToConsoleArea("ERR: Arithmetic Overflow Error");
                    return false;
                } else {
                    if (stackRegister.size() > 5) {
                        embedToConsoleArea("ERR: Stack Overflow");
                        return false;
                    } else {
                        stackRegister.push(diff);
                    }
                }
            }
        } else if (mem.startsWith("06")) {
            if (stackRegister.size() < 2) {
                embedToConsoleArea("ERR: Null Compare Error");
                return false;
            } else {
                int top1 = stackRegister.pop();
                int top2 = stackRegister.pop();
                if (top1 == top2) {
                    if (stackRegister.size() > 5) {
                        embedToConsoleArea("ERR: Stack Overflow");
                        return false;
                    } else {
                        stackRegister.push(1);
                    }
                } else {
                    if (stackRegister.size() > 5) {
                        embedToConsoleArea("ERR: Stack Overflow");
                        return false;
                    } else {
                        stackRegister.push(0);
                    }
                }
            }
        }
        return true;
    }
    
    public String getMachineCodes() {
        String[] machineCodes = new String[referenceTable.size()];
        for (int i = 0; i < machineCodes.length; i++) {
            machineCodes[i] = (String) referenceTable.get(i)[3];
        }
        String toWrite = Arrays.toString(machineCodes);
        toWrite = toWrite.replace(", ", "\n");
        toWrite = toWrite.replace("[", "");
        toWrite = toWrite.replace("]", "");
        return toWrite;
    }
    
    public void embedToMemoryArea() {
        String toMemArea = "";
        for (int i = 0; i < memory.size(); i++) {
            toMemArea += i + ": " + (String) memory.get(i) + "\n";
        }
        parentFrame.getMemTextArea().setText(toMemArea);
    }
    
    public void embedToStackRegArea() {
        String toStackRegArea = "";
        for (int i = 0; i < stackRegister.size(); i++) {
            toStackRegArea += i + ": " + stackRegister.get(i) + "\n";
        }
        parentFrame.getStackRegTextArea().setText(toStackRegArea);
    }
    
    public void embedToConsoleArea(String toAppend) {
        consoleText += toAppend + "\n";
        if (toAppend.startsWith("ERR:")) {
            JOptionPane.showMessageDialog(null, toAppend);
            consoleText += "EXECUTION FINISHED.\nPress `RESET` to continue...";
        }
        parentFrame.getConsoleTextArea().setText(consoleText);
    }
    
    public void write() throws IOException {
        String[] machineCodes = new String[referenceTable.size()];
        for (int i = 0; i < machineCodes.length; i++) {
            machineCodes[i] = (String) referenceTable.get(i)[3];
        }
        FileWriter fw = new FileWriter("machinecode.txt");
        String toWrite = Arrays.toString(machineCodes);
        toWrite = toWrite.replace(", ", "\n");
        toWrite = toWrite.replace("[", "");
        toWrite = toWrite.replace("]", "");
        fw.write(toWrite);
        fw.close();
    }
    
    public static void main(String[] args) throws IOException {
        AssemBlurt asm = new AssemBlurt("assemblycode2.txt");
        asm.parseInstructions();
        asm.write();
        asm.executeAllMachineCodes();
    }
    
}
