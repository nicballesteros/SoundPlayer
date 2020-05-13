import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Sound {

    private AudioFormat format;
    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    public final Object streamKey = new Object();

    public static void main(String[] args) {
        new Sound();
    }

    public Sound() {
        format = new AudioFormat(8000, 8, 1, true, true);

        outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[32];
        inputStream = new ByteArrayInputStream(buf);

        Thread record = new Thread(new AudioRecorder(outputStream));
        Thread listen = new Thread(new AudioListener(inputStream));

        record.start();
        listen.start();

        while(true) {
            synchronized (streamKey) {
                byte[] buffer = outputStream.toByteArray();
                inputStream = new ByteArrayInputStream(buffer);
            }
        }
    }

    private class AudioRecorder implements Runnable {
        private AtomicBoolean running;

        public final Object runningKey = new Object();

        private OutputStream outputStream;

        private BufferedOutputStream bufferedOutputStream;

        public AudioRecorder(OutputStream outputStream) { // constructor run when normal call
            this.running = new AtomicBoolean(true);
            this.outputStream = outputStream;
            this.bufferedOutputStream = new BufferedOutputStream(this.outputStream);
        }

        @Override
        public void run() {
            try {
                TargetDataLine line;

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);

                line.open(format);
                line.start();

                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte[] buffer = new byte[bufferSize];

                while(this.running.get()) {
//                    System.out.println("Recording");
                    //do a record

                    //send to server a packet
                    int count = line.read(buffer, 0, bufferSize);

                    try {
                        //write the audio data from the mic to the server
                        synchronized (streamKey) {
                            bufferedOutputStream.write(buffer);
                        }
//                        objectOutputStream.write(1);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
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
        public AudioListener(InputStream inputStream) {
            this.running = new AtomicBoolean(true);
            this.inputStream = inputStream;
            this.bufferedInputStream = new BufferedInputStream(inputStream);
        }

        @Override
        public void run() {
            try {
                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte[] buffer = new byte[bufferSize];

                while (this.running.get()) {
                    //listen to server and play in speaker
                    synchronized (streamKey) {
                        bufferedInputStream.read(buffer, 0, buffer.length);
                    }

                    //play to speakers
                    InputStream is = new ByteArrayInputStream(buffer);
                    AudioInputStream ais = AudioSystem.getAudioInputStream(is);
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.start();
                    //Thread.sleep(clip.getMicrosecondLength());
                    clip.stop();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (UnsupportedAudioFileException unsupportedAudioFileException) {
                unsupportedAudioFileException.printStackTrace();
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
