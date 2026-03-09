Add-Type -TypeDefinition @"
using System;
using System.IO;

public class SoundGen {
    public static void Gen(string path) {
        int sampleRate = 44100;
        double duration = 0.4;
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
                
                // Attack (fast) and Decay (slower)
                double env = 1.0;
                if (t < 0.05) env = t / 0.05;
                else env = Math.Max(0, 1.0 - (t - 0.05) / 0.35);
                
                // Base impact thud - descending pitch 150hz -> 40hz
                double freq = 150 - 110 * (t / duration);
                double val = Math.Sin(freq * 2 * Math.PI * t);
                
                // Add some crunch (noise) during the high energy part
                if (t < 0.15) {
                    val += (rnd.NextDouble() * 2.0 - 1.0) * 0.8;
                }
                
                val *= env;
                
                short sample = (short)(Math.Max(-1.0, Math.Min(1.0, val)) * 32767);
                bw.Write(sample);
            }
        }
    }
}
"@
[SoundGen]::Gen("app\src\main\res\raw\punch.wav")
