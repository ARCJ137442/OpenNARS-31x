/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import nars.io.OutputChannel;

/**
 * @author Pei, Xiang, Patrick, Peter
 */
public class Shell {

    static String inputString = "";

    private static class InputThread extends Thread {
        private final BufferedReader bufIn;
        private final NAR reasoner;

        InputThread(final InputStream in, NAR reasoner) {
            this.bufIn = new BufferedReader(new InputStreamReader(in));
            this.reasoner = reasoner;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final String line = bufIn.readLine();
                    if (line != null) {
                        synchronized (inputString) {
                            inputString = line;
                            // if("".equals(line)) {
                            // inputString = "1";
                            // }
                        }
                    }

                } catch (final IOException e) {
                    throw new IllegalStateException("Could not read line.", e);
                }

                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException("Unexpectadly interrupted while sleeping.", e);
                }
            }
        }
    }

    public static class ShellOutput implements OutputChannel {
        @Override
        public void nextOutput(ArrayList<String> arg0) {
            for (String s : arg0) {
                if (!s.strip().matches("[0-9]+")) {
                    System.out.println(s);
                }
            }
        }

        @Override
        public void tickTimer() {
        }
    }

    public static void main(String[] args) {
        NAR reasoner = new NAR();
        reasoner.addOutputChannel(new ShellOutput());
        InputThread t = new InputThread(System.in, reasoner);
        t.start();
        System.out.println("Welcome to OpenNARS v" + Parameters.VERSION +
                " Shell! Type Narsese input and press enter, use questions to get "
                + "answers or increase volume with *volume=n with n=0..100");
        reasoner.run();
        reasoner.getSilenceValue().set(0);
        while (true) {
            synchronized (inputString) {
                if (!"".equals(inputString)) {
                    try {
                        // 退出程序
                        // * 🎯【2024-05-09 13:35:47】在其它语言中通过`java -jar`启动OpenNARS时，主动退出不容易——总是有残余进程
                        if (inputString.startsWith("*exit") || inputString.startsWith("*quit")) {
                            System.out.println("TERMINATED: OpenNARS exited by command \"" + inputString + "\".");
                            System.exit(0);
                        }
                        // 推理步进（手动）
                        else if (inputString.matches("[0-9]+")) {
                            System.out.println("INFO: running " + inputString + " cycles.");
                            int val = Integer.parseInt(inputString);
                            for (int i = 0; i < val; i++)
                                reasoner.cycle();
                        }
                        // 音量相关
                        else if (inputString.startsWith("*volume")) { // volume to be consistent with OpenNARS
                            String[] splits = inputString.split("=");
                            // 查看音量
                            if (splits.length <= 1) {
                                System.out.println("INFO: *volume = " + (100 - reasoner.getSilenceValue().get()));
                            }
                            // 设置音量
                            else {
                                int val = Integer.parseInt(splits[1]);
                                if (val >= 0 && val <= 100) {
                                    reasoner.getSilenceValue().set(100 - val);
                                } else {
                                    System.out.println("INFO: Volume ignored, not in range");
                                }
                            }
                        }
                        // 开启debug模式
                        else if (inputString.startsWith("*debug=")) {
                            String param = inputString.split("\\*debug=")[1];
                            NAR.DEBUG = !param.isEmpty();
                        }
                        // 输入Narsese
                        else {
                            reasoner.textInputLine(inputString);
                            reasoner.cycle(); // 输入之后至少推理步进一步
                        }
                        inputString = "";
                    } catch (Exception ex) {
                        inputString = "";
                    }
                }
            }
            if (reasoner.getWalkingSteps() > 0)
                reasoner.cycle();
        }
    }
}
