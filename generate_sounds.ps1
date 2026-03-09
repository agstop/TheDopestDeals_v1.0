Add-Type -TypeDefinition @"
using System;
using System.IO;

public class SoundGen {
    public static void GenCashRegister(string path) {
        int sampleRate = 44100;
        double duration = 0.5;
        int numSamples = (int)(sampleRate * duration);
        using (FileStream fs = new FileStream(path, FileMode.Create))
        using (BinaryWriter bw = new BinaryWriter(fs)) {
            bw.Write(new char[4] { 'R', 'I', 'F', 'F' });
            bw.Write(36 + numSamples * 2);
            bw.Write(new char[4] { 'W', 'A', 'V', 'E' });
            bw.Write(new char[4] { 'f', 'm', 't', ' ' });
            bw.Write(16);
            bw.Write((short)1);
            bw.Write((short)1);
            bw.Write(sampleRate);
            bw.Write(sampleRate * 2);
            bw.Write((short)2);
            bw.Write((short)16);
            bw.Write(new char[4] { 'd', 'a', 't', 'a' });
            bw.Write(numSamples * 2);
            
            Random rnd = new Random();
            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / sampleRate;
                double val = 0.0;
                
                if (t < 0.1) {
                    double freq = 2500 - 15000 * t;
                    double env = 1.0 - t/0.1;
                    val = Math.Sin(freq * 2 * Math.PI * t) * env;
                } else if (t < 0.4) {
                    double freq = 2000 + rnd.NextDouble() * 2000;
                    double env = Math.Exp(-15 * (t - 0.1));
                    val = Math.Sin(freq * 2 * Math.PI * t) * env;
                }
                
                short sample = (short)(Math.Max(-1.0, Math.Min(1.0, val)) * 32767);
                bw.Write(sample);
            }
        }
    }
    
    public static void GenChuckle(string path) {
        int sampleRate = 44100;
        double duration = 1.0;
        int numSamples = (int)(sampleRate * duration);
        using (FileStream fs = new FileStream(path, FileMode.Create))
        using (BinaryWriter bw = new BinaryWriter(fs)) {
            bw.Write(new char[4] { 'R', 'I', 'F', 'F' });
            bw.Write(36 + numSamples * 2);
            bw.Write(new char[4] { 'W', 'A', 'V', 'E' });
            bw.Write(new char[4] { 'f', 'm', 't', ' ' });
            bw.Write(16);
            bw.Write((short)1);
            bw.Write((short)1);
            bw.Write(sampleRate);
            bw.Write(sampleRate * 2);
            bw.Write((short)2);
            bw.Write((short)16);
            bw.Write(new char[4] { 'd', 'a', 't', 'a' });
            bw.Write(numSamples * 2);
            
            Random rnd = new Random();
            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / sampleRate;
                double val = 0.0;
                
                double beat = (t * 6) % 1.0;
                if (beat < 0.3) {
                    double env = Math.Sin(beat / 0.3 * Math.PI);
                    double freq = 150 + (rnd.NextDouble() * 40 - 20);
                    val = Math.Sin(freq * 2 * Math.PI * t) * env;
                    val += (rnd.NextDouble() * 0.2 - 0.1) * env;
                }
                
                val *= 0.5;
                short sample = (short)(Math.Max(-1.0, Math.Min(1.0, val)) * 32767);
                bw.Write(sample);
            }
        }
    }
}
"@

[SoundGen]::GenCashRegister("app\src\main\res\raw\cash_register.wav")
[SoundGen]::GenChuckle("app\src\main\res\raw\chuckle.wav")
