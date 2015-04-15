#pragma version(1)
#pragma rs java_package_name(com.example.android.renderscriptimaging)

float centerX;
float centerY;
float minRadius;

//Amplitude control of the wave peaks
float scalar;
//Wave Dampener, larger values damp out the ripples sooner
float damper;
//Sine frequency, larger values show more ripples
float frequency;

void root(const uchar4* v_in, uchar4* v_out, const void* usrData,
        uint32_t x, uint32_t y)
{
    //Compute distance from the center
    float dx = x - centerX;
    float dy = y - centerY;
    float radius = sqrt(dx*dx + dy*dy);

    if (radius < minRadius)
    {
        //Use the original pixel
        *v_out = *v_in;
    }
    else
    {
        float4 f4 = rsUnpackColor8888(*v_in);
        float shiftedRadius = radius - minRadius;

        //Determine sine function multiplier based on distance
        float multiplier = (scalar * exp(-shiftedRadius * damper)
                * -sin(shiftedRadius * frequency)) + 1;
        //Lighten or darken pixel
        float3 transformed = f4.rgb * multiplier;
        *v_out = rsPackColorTo8888(transformed);
    }
}