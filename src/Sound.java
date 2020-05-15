import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.sound.sampled.AudioSystem.getMixer;
import static javax.sound.sampled.AudioSystem.getMixerInfo;

public class Sound {

    private AudioFormat format;
    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    public final Object streamKey = new Object();
    private Mixer mixer;

    public static void main(String[] args) throws InterruptedException{
        new Sound();
    }

    public Sound() throws InterruptedException {
        format = new AudioFormat(44100.0f, 16, 2, true, true);
        mixer = getMixer(getMixerInfo()[10]);

        Line.Info[] info = mixer.getTargetLineInfo();
        Line.Info[] speakInfo = mixer.getSourceLineInfo();

        try {
            SourceDataLine speakers = (SourceDataLine) mixer.getLine(speakInfo[0]);
            TargetDataLine microphone = (TargetDataLine) mixer.getLine(info[0]);

            AudioHandler runnable = new AudioHandler(speakers, microphone);
            Thread audio = new Thread(runnable, "Audio-Handles");

            audio.start();

        } catch (LineUnavailableException lineUnavailableException) {
            lineUnavailableException.printStackTrace();
        }

        while(true) {
            System.out.println("Recording and playing and sending data and stuff");
            try {
                Thread.sleep(100);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }



    private class AudioHandler implements Runnable {
        private TargetDataLine microphone;
        private SourceDataLine speakers;
        private AtomicBoolean running;

        private byte[] buffer;

        public AudioHandler() {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                this.microphone = (TargetDataLine) AudioSystem.getLine(info);

                this.running = new AtomicBoolean(true);

                info = new DataLine.Info(SourceDataLine.class, format);
                this.speakers = (SourceDataLine) AudioSystem.getLine(info);
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
        }

        public AudioHandler(SourceDataLine speakers, TargetDataLine microphone) {
            this.microphone = microphone;
            this.speakers = speakers;
            this.running = new AtomicBoolean(true);
        }

        @Override
        public void run() {
            int chunk = 1024;

            this.buffer = new byte[chunk];


            try {
                speakers.open(format);
                microphone.open(format);

                microphone.start();
                speakers.start();
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }

            while(running.get()) {
                int count = microphone.read(this.buffer, 0, chunk);
                speakers.write(this.buffer, 0, count);
            }
        }

        public boolean isRunning() {
            return running.get();
        }

        public void stopAllAudio() {
            running.set(false);
        }
    }


    //Everything below here is not proven to work.

    private class AudioRecorder implements Runnable {
        private AtomicBoolean running;

        private ByteArrayInputStream inputStream;
       // private BufferedOutputStream bufferedOutputStream;

        private byte[] buffer;

        public AudioRecorder() { // constructor run when normal call
            this.running = new AtomicBoolean(true);
//            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                TargetDataLine line;
                //byte[] buffer;
                synchronized (streamKey) {
                    //DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    line = (TargetDataLine) mixer.getLine(mixer.getTargetLineInfo()[0]);

                    line.open(format);
                    line.start();

//                    int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
//                    buffer = new byte[bufferSize];
                }


                while(this.running.get()) {
//                    System.out.println("Recording");
                    //do a record

                    //send to server a packet
                    synchronized (streamKey) {
                        byte[] buffer = new byte[(int)format.getSampleRate() * format.getFrameSize()];
                        int count = line.read(buffer, 0, buffer.length);
                        System.out.println("Recording");
                        inputStream = new ByteArrayInputStream(buffer);
                        streamKey.notify();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
//                    try {
//                        //write the audio data from the mic to the server
//                        synchronized (streamKey) {
////                            bufferedOutputStream.write(buffer);
//                            System.out.println("write");
//                        }
////                        objectOutputStream.write(1);
//                    } catch (IOException ioException) {
//                        ioException.printStackTrace();
//                    }
                }
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
        }

        public void stopRecording() {
            this.running.set(false);
        }

        public boolean isRunning() {
            return running.get();
        }
    }

    private class AudioListener implements Runnable {

        private AtomicBoolean running;
        private InputStream inputStream;
        private BufferedInputStream bufferedInputStream;


        private byte[] buffer;

        public AudioListener() {
            this.running = new AtomicBoolean(true);

        }


//        public AudioListener(InputStream inputStream) {
//            this.running = new AtomicBoolean(true);
//            this.inputStream = inputStream;
//            this.bufferedInputStream = new BufferedInputStream(inputStream);
//        }

        @Override
        public void run() {
            try {

//                byte[] buffer;
                SourceDataLine speakers;

                synchronized (streamKey) {
                    //int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                    //buffer = new byte[bufferSize];

                    //DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                    speakers = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]);
                    speakers.open(format);
                    speakers.start();
                }

                while (this.running.get()) {
                    //listen to server and play in speaker
                    synchronized (streamKey) {
//                        bufferedInputStream.read(this.buffer, 0, buffer.length);
//                        ByteArrayInputStream baiss = new ByteArrayInputStream(buffer);
//                        AudioInputStream ais = new AudioInputStream(baiss, format, buffer.length);
                        try {
                            if(inputStream == null) {
                                streamKey.wait();
                                continue;
                            }

                            if (inputStream.available() == 0) {
                                streamKey.wait();
                            }


                            byte[] buffer = new byte[(int)format.getSampleRate() * format.getFrameSize()];
                            inputStream.read(buffer, 0, buffer.length);
                            speakers.write(this.buffer, 0, this.buffer.length);
                            System.out.println("Playing");

                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }

                    //play to speakers
                }
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
        }

        public void stopListening() {
            this.running.set(false);
        }

        public boolean isRunning() {
            return this.running.get();
        }
    }
}
