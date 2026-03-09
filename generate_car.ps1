Add-Type -TypeDefinition @"
using System;
using System.IO;

public class SoundGen {
    public static void Gen(string path) {
        int sampleRate = 44100;
        double duration = 2.5;
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
            double phase = 0;
            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / sampleRate;
                double freq = 40;
                if (t < 1.0) freq = 60 + 80 * t;
                else if (t < 2.0) freq = 140 - 100 * (t - 1.0);
                else freq = 40 - 10 * (t - 2.0);
                
                phase += freq * 2 * Math.PI / sampleRate;
                
                double val = Math.Sin(phase) > 0 ? 1.0 : -1.0;
                val += 0.5 * Math.Sin(phase * 2.5);
                val += (rnd.NextDouble() * 0.6 - 0.3);
                
                double env = 1.0;
                if (t < 0.2) env = t / 0.2;
                else if (t > 2.0) env = 1.0 - (t - 2.0) / 0.5;
                
                if (t > 2.0 && t < 2.3) {
                    double squFreq = 2000 - 500 * (t - 2.0);
                    val += Math.Sin(squFreq * 2 * Math.PI * t) * 0.4;
                }
                
                val *= env;
                short sample = (short)(Math.Max(-1.0, Math.Min(1.0, val / 2.0)) * 32767);
                bw.Write(sample);
            }
        }
    }
}
"@
[SoundGen]::Gen("app\src\main\res\raw\car_travel.wav")
