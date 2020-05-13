import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Sound {

    private AudioFormat format;
    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    public final Object streamKey = new Object();

    public static void main(String[] args) throws InterruptedException{
        new Sound();
    }

    public Sound() throws InterruptedException {
        format = new AudioFormat(44100, 16, 2, true, true);


//        outputStream = new ByteArrayOutputStream((int) format.getSampleRate() * format.getFrameSize());
        byte[] buf = new byte[(int)format.getSampleRate() * format.getFrameSize()];
//        inputStream = new ByteArrayInputStream(buf);

        Thread record = new Thread(new AudioRecorder(buf));
        Thread listen = new Thread(new AudioListener(buf));

        record.start();
        listen.start();

        while(true);

//        while(true) {
//            synchronized (streamKey) {
//                byte[] buffer = outputStream.toByteArray();
//                inputStream = new ByteArrayInputStream(buffer);
//            }
//            Thread.sleep(10);
//        }
    }

    private class AudioRecorder implements Runnable {
        private AtomicBoolean running;

        private OutputStream outputStream;
        private BufferedOutputStream bufferedOutputStream;

        private byte[] buffer;

//        public AudioRecorder(OutputStream outputStream) { // constructor run when normal call
//            this.running = new AtomicBoolean(true);
//            this.outputStream = outputStream;
//            this.bufferedOutputStream = new BufferedOutputStream(this.outputStream);
//        }

        public AudioRecorder(byte[] buffer) { // constructor run when normal call
            this.running = new AtomicBoolean(true);
            //this.outputStream = outputStream;
//            this.bufferedOutputStream = new BufferedOutputStream(this.outputStream);
            synchronized (streamKey) {
                this.buffer = buffer;
            }
        }

        @Override
        public void run() {
            try {
                TargetDataLine line;
                //byte[] buffer;
                synchronized (streamKey) {
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    line = (TargetDataLine) AudioSystem.getLine(info);

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
                        int count = line.read(this.buffer, 0, this.buffer.length);
                        System.out.println("Recording");
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

        public AudioListener(byte[] buffer) {
            this.running = new AtomicBoolean(true);
            synchronized (streamKey) {
                this.buffer = buffer;
            }
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

                    DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                    speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                    speakers.open(format);
                    speakers.start();
                }




                while (this.running.get()) {
                    //listen to server and play in speaker
                    synchronized (streamKey) {
//                        bufferedInputStream.read(this.buffer, 0, buffer.length);
//                        ByteArrayInputStream baiss = new ByteArrayInputStream(buffer);
//                        AudioInputStream ais = new AudioInputStream(baiss, format, buffer.length);
                        System.out.println("Playing");
                        speakers.write(this.buffer, 0, this.buffer.length);
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
