package net.zentring.live;

import com.pedro.encoder.input.audio.CustomAudioEffect;

public class AudioVolume extends CustomAudioEffect {
    private int volumeScale = 100;

    private int volumeRealTime = 0;

    public void setVolume(int scale) {
        volumeScale = scale;
    }

    public int getVolume() {
        return volumeRealTime;
    }

    @Override
    public byte[] process(byte[] pcmBuffer) {
        byte[] result = new byte[pcmBuffer.length];
        amplifyPCMData(pcmBuffer, pcmBuffer.length, result, 16, (volumeScale / 100.0f));
        return result;
    }

    private short getShort(byte[] data, int start) {
        return (short) ((data[start] & 0xFF) | (data[start + 1] << 8));
    }

    short SHRT_MAX = (short) 0x7F00;
    short SHRT_MIN = (short) -0x7F00;
    int center = Short.MAX_VALUE / 2;

    //调节PCM数据音量
    int amplifyPCMData(byte[] pData, int nLen, byte[] data2, int nBitsPerSample, float multiple) {
        int nCur = 0;
        if (16 == nBitsPerSample) {
            long volumeT = 0;
            int counter = 0;
            while (nCur < nLen - 1) {
                counter++;
                short volum = getShort(pData, nCur);
                //Log.d(TAG, "volum="+volum);
                volum = (short) (volum * multiple);

                //爆音處理
                if (volum < SHRT_MIN) {
                    volum = SHRT_MIN;
                } else if (volum > SHRT_MAX) {
                    volum = SHRT_MAX;
                }

//                int fixed = volum - center;
//                volumeT += (int) ((fixed / (float) center) * 100);
                volumeT += Math.abs(volum);

                data2[nCur] = (byte) (volum & 0xFF);
                data2[nCur + 1] = (byte) ((volum >> 8) & 0xFF);
                nCur += 2;
            }
            volumeRealTime = (int) (volumeT / counter) / 10;

        }
        return 0;
    }
}
