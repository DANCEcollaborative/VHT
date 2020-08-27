using System;
using System.Collections.Generic;
using System.Xml;
using System.IO;
using Amazon.Polly;
using Amazon.Polly.Model;
using Newtonsoft.Json;

namespace TtsRelay
{
    class AWSPollyRelay : ITtsRelay
    {
        // http://docs.aws.amazon.com/polly/latest/dg/using-speechmarks.html
        class SpeechMark
        {
            public double time = 0; // the timestamp in milliseconds from the beginning of the corresponding audio stream
            public string type = ""; // the type of speech mark (sentence, word, viseme, or ssml).
            public int start = 0; // the offset in bytes of the start of the object in the input text (not including viseme marks)
            public int end = 0; // the offset in bytes of the object's end in the input text (not including viseme marks)
            public string value = ""; // this varies depending on the type of speech mark- SSML: <mark> SSML tag | viseme: the viseme name | word or sentence: a substring of the input text, as delimited by the start and end fields
        }

        class IPAtoFacefxMap
        {
            readonly public string ipaPhoneme = "";
            readonly public string[] facefxVisemes;
            readonly public float[] amounts;

            public IPAtoFacefxMap(string _ipaPhoneme, string _facefxViseme, float _amount)
            {
                ipaPhoneme = _ipaPhoneme;
                facefxVisemes = new string[] { _facefxViseme } ;
                amounts = new float[] { _amount } ;
            }

            public IPAtoFacefxMap(string _ipaPhoneme, string[] _facefxVisemes, float[] _amounts)
            {
                ipaPhoneme = _ipaPhoneme;
                facefxVisemes = _facefxVisemes;
                amounts = _amounts;
            }
        }


        #region FacefxMapping
      
        const string facefxMapping =
       @"<mapping>
          <entry phoneme='@' target='open' amount='0.400000' />
          <entry phoneme='sil' target='open' amount='0.000000' />
          <entry phoneme='t' target='open' amount='0.400000' />
          <entry phoneme='d' target='open' amount='0.400000' />
          <entry phoneme='k' target='open' amount='0.250000' />
          <entry phoneme='g' target='open' amount='0.250000' />
          <entry phoneme='n' target='open' amount='0.400000' />
          <entry phoneme='ng' target='open' amount='0.400000' />
          <entry phoneme='RA' target='open' amount='0.400000' />
          <entry phoneme='RU' target='open' amount='0.250000' />
          <entry phoneme='FLAP' target='open' amount='0.300000' />
          <entry phoneme='PH' target='open' amount='0.100000' />
          <entry phoneme='th' target='open' amount='0.450000' />
          <entry phoneme='DH' target='open' amount='0.450000' />
          <entry phoneme='s' target='open' amount='0.150000' />
          <entry phoneme='z' target='open' amount='0.150000' />
          <entry phoneme='CX' target='open' amount='0.250000' />
          <entry phoneme='X' target='open' amount='0.250000' />
          <entry phoneme='GH' target='open' amount='0.250000' />
          <entry phoneme='hh' target='open' amount='0.300000' />
          <entry phoneme='r' target='open' amount='0.100000' />
          <entry phoneme='l' target='open' amount='0.400000' />
          <entry phoneme='H' target='open' amount='0.200000' />
          <entry phoneme='TS' target='open' amount='0.400000' />
          <entry phoneme='iy' target='open' amount='0.200000' />
          <entry phoneme='E' target='open' amount='0.350000' />
          <entry phoneme='EN' target='open' amount='0.350000' />
          <entry phoneme='eh' target='open' amount='0.500000' />
          <entry phoneme='a' target='open' amount='0.550000' />
          <entry phoneme='A' target='open' amount='0.550000' />
          <entry phoneme='aa' target='open' amount='0.550000' />
          <entry phoneme='AAN' target='open' amount='0.550000' />
          <entry phoneme='ao' target='open' amount='0.400000' />
          <entry phoneme='AON' target='open' amount='0.400000' />
          <entry phoneme='O' target='open' amount='0.400000' />
          <entry phoneme='ON' target='open' amount='0.400000' />
          <entry phoneme='uw' target='open' amount='0.400000' />
          <entry phoneme='EU' target='open' amount='0.400000' />
          <entry phoneme='OE' target='open' amount='0.400000' />
          <entry phoneme='OEN' target='open' amount='0.400000' />
          <entry phoneme='ah' target='open' amount='0.500000' />
          <entry phoneme='ih' target='open' amount='0.500000' />
          <entry phoneme='UU' target='open' amount='0.400000' />
          <entry phoneme='uh' target='open' amount='0.400000' />
          <entry phoneme='ax' target='open' amount='0.500000' />
          <entry phoneme='UX' target='open' amount='0.500000' />
          <entry phoneme='ae' target='open' amount='0.500000' />
          <entry phoneme='er' target='open' amount='0.400000' />
          <entry phoneme='AXR' target='open' amount='0.400000' />
          <entry phoneme='EXR' target='open' amount='0.400000' />
          <entry phoneme='ey' target='open' amount='0.500000' />
          <entry phoneme='aw' target='open' amount='0.500000' />
          <entry phoneme='ay' target='open' amount='0.500000' />
          <entry phoneme='oy' target='open' amount='0.400000' />
          <entry phoneme='ow' target='open' amount='0.400000' />
          <entry phoneme='r' target='W' amount='0.700000' />
          <entry phoneme='y' target='W' amount='0.500000' />
          <entry phoneme='w' target='W' amount='0.850000' />
          <entry phoneme='ao' target='W' amount='0.550000' />
          <entry phoneme='AON' target='W' amount='0.550000' />
          <entry phoneme='O' target='W' amount='0.550000' />
          <entry phoneme='ON' target='W' amount='0.550000' />
          <entry phoneme='uw' target='W' amount='0.550000' />
          <entry phoneme='UY' target='W' amount='0.850000' />
          <entry phoneme='EU' target='W' amount='0.550000' />
          <entry phoneme='OE' target='W' amount='0.550000' />
          <entry phoneme='OEN' target='W' amount='0.550000' />
          <entry phoneme='UU' target='W' amount='0.550000' />
          <entry phoneme='uh' target='W' amount='0.550000' />
          <entry phoneme='oy' target='W' amount='0.550000' />
          <entry phoneme='ow' target='W' amount='0.550000' />
          <entry phoneme='sh' target='ShCh' amount='0.850000' />
          <entry phoneme='zh' target='ShCh' amount='0.850000' />
          <entry phoneme='y' target='ShCh' amount='0.300000' />
          <entry phoneme='ch' target='ShCh' amount='0.850000' />
          <entry phoneme='jh' target='ShCh' amount='0.850000' />
          <entry phoneme='er' target='ShCh' amount='0.500000' />
          <entry phoneme='AXR' target='ShCh' amount='0.500000' />
          <entry phoneme='EXR' target='ShCh' amount='0.500000' />
          <entry phoneme='p' target='PBM' amount='0.900000' />
          <entry phoneme='b' target='PBM' amount='0.900000' />
          <entry phoneme='m' target='PBM' amount='0.900000' />
          <entry phoneme='PH' target='FV' amount='0.400000' />
          <entry phoneme='f' target='FV' amount='0.750000' />
          <entry phoneme='v' target='FV' amount='0.750000' />
          <entry phoneme='s' target='wide' amount='0.500000' />
          <entry phoneme='z' target='wide' amount='0.500000' />
          <entry phoneme='iy' target='wide' amount='0.800000' />
          <entry phoneme='E' target='wide' amount='0.250000' />
          <entry phoneme='EN' target='wide' amount='0.250000' />
          <entry phoneme='eh' target='wide' amount='0.600000' />
          <entry phoneme='ah' target='wide' amount='0.600000' />
          <entry phoneme='ih' target='wide' amount='0.600000' />
          <entry phoneme='ax' target='wide' amount='0.600000' />
          <entry phoneme='UX' target='wide' amount='0.600000' />
          <entry phoneme='ae' target='wide' amount='0.600000' />
          <entry phoneme='ey' target='wide' amount='0.600000' />
          <entry phoneme='aw' target='wide' amount='0.600000' />
          <entry phoneme='ay' target='wide' amount='0.600000' />
          <entry phoneme='k' target='tBack' amount='0.800000' />
          <entry phoneme='g' target='tBack' amount='0.800000' />
          <entry phoneme='RU' target='tBack' amount='0.800000' />
          <entry phoneme='CX' target='tBack' amount='0.800000' />
          <entry phoneme='X' target='tBack' amount='0.800000' />
          <entry phoneme='GH' target='tBack' amount='0.800000' />
          <entry phoneme='eh' target='tBack' amount='0.400000' />
          <entry phoneme='ah' target='tBack' amount='0.400000' />
          <entry phoneme='ih' target='tBack' amount='0.400000' />
          <entry phoneme='ax' target='tBack' amount='0.400000' />
          <entry phoneme='UX' target='tBack' amount='0.400000' />
          <entry phoneme='ae' target='tBack' amount='0.400000' />
          <entry phoneme='ey' target='tBack' amount='0.400000' />
          <entry phoneme='aw' target='tBack' amount='0.400000' />
          <entry phoneme='ay' target='tBack' amount='0.400000' />
          <entry phoneme='t' target='tRoof' amount='0.800000' />
          <entry phoneme='d' target='tRoof' amount='0.800000' />
          <entry phoneme='n' target='tRoof' amount='0.800000' />
          <entry phoneme='ng' target='tRoof' amount='0.800000' />
          <entry phoneme='RA' target='tRoof' amount='0.500000' />
          <entry phoneme='FLAP' target='tRoof' amount='0.600000' />
          <entry phoneme='s' target='tRoof' amount='0.400000' />
          <entry phoneme='z' target='tRoof' amount='0.400000' />
          <entry phoneme='sh' target='tRoof' amount='0.400000' />
          <entry phoneme='zh' target='tRoof' amount='0.400000' />
          <entry phoneme='y' target='tRoof' amount='0.400000' />
          <entry phoneme='l' target='tRoof' amount='0.800000' />
          <entry phoneme='TS' target='tRoof' amount='0.800000' />
          <entry phoneme='ch' target='tRoof' amount='0.400000' />
          <entry phoneme='jh' target='tRoof' amount='0.400000' />
          <entry phoneme='iy' target='tRoof' amount='0.200000' />
          <entry phoneme='E' target='tRoof' amount='0.200000' />
          <entry phoneme='EN' target='tRoof' amount='0.200000' />
          <entry phoneme='er' target='tRoof' amount='0.500000' />
          <entry phoneme='AXR' target='tRoof' amount='0.500000' />
          <entry phoneme='EXR' target='tRoof' amount='0.500000' />
          <entry phoneme='th' target='tTeeth' amount='0.900000' />
          <entry phoneme='DH' target='tTeeth' amount='0.900000' />
       </mapping>"
    ;
        #endregion

        AmazonPollyClient client;
        string visemeMappingType = "facefx";
        //XmlDocument facefxMappingDoc = null;
        Dictionary<string, IPAtoFacefxMap> m_IPAtoFacefxMap = new Dictionary<string, IPAtoFacefxMap>(); // the key is the ipa
        private GenerateAudioReply generateAudioReply = null;

        public bool GenerateAudio(string message, string outputFileName, string messageOutputFileName, string voice, ref string xmlReplyReturn, ref GenerateAudioReply generateAudioReplyReturn)
        {
            bool success = true;

            xmlReplyReturn = "";

            // In GetVoices(), we replace the space in the name with a pipe '|' because we can't select a voice with a space in the name (because it's a vhmsg sbm command).
            // Here, we put the space back in, so that we can correctly select the voice via the SAPI function SelectVoice()
            voice = voice.Replace("|", " ");
            voice = voice.Replace("Polly_", "");

            SynthesizeSpeechRequest audioRequest = new SynthesizeSpeechRequest {
                /*LexiconNames = new List<string> {
                "example"
                },*/
                OutputFormat = "ogg_vorbis",
                SampleRate = "22050",
                Text = message,
                TextType = "ssml",//"text",
                VoiceId = voice,
            };

            SynthesizeSpeechRequest markRequest = new SynthesizeSpeechRequest
            {
                OutputFormat = "json",
                SampleRate = "22050",
                Text = message,
                TextType = "ssml",
                VoiceId = voice,
                SpeechMarkTypes = new List<string> { "sentence", "viseme", "word" } 
            };

            SynthesizeSpeechResponse audioResponse = client.SynthesizeSpeech(audioRequest);
            SynthesizeSpeechResponse markResponse = client.SynthesizeSpeech(markRequest);

            if (audioResponse != null && markResponse != null)
            {
                Stream audioStream = audioResponse.AudioStream;
                Stream markStream = markResponse.AudioStream;

                //response.
                //string contentType = response.ContentType;
                //int requestCharacters = response.RequestCharacters;

                outputFileName = Path.ChangeExtension(outputFileName, "ogg");

                using (Stream file = File.Create(outputFileName))
                {
                    CopyStream(audioStream, file);
                }

                List<SpeechMark> marks = new List<SpeechMark>();

                // convert the stream so that we can read it
                using (StreamReader reader = new StreamReader(markStream))
                {
                    // read the marks, line by line from the stream we received
                    while (!reader.EndOfStream)
                    {
                        string line = reader.ReadLine();

                        if (string.IsNullOrEmpty(line))
                        {
                            continue;
                        }

                        marks.Add(JsonConvert.DeserializeObject<SpeechMark>(line));
                    }
                }

                generateAudioReply = generateAudioReplyReturn;
                generateAudioReply.used = true;
                generateAudioReply.soundFile = outputFileName;
                generateAudioReply.WordBreakList = new List<KeyValuePairS<double, double>>();
                generateAudioReply.MarkList = new List<KeyValuePairS<string, double>>();
                generateAudioReply.VisemeList = new List<GenerateAudioReplyViseme>();

                ParseMarks(marks, ref generateAudioReplyReturn);
            }
            else
            {
                Console.WriteLine("Failed to generate audio");
            }

            return success;
        }

        /// <summary>
        /// Convert the aws speech marks into proper TtsRelay output data
        /// </summary>
        /// <param name="marks"></param>
        /// <param name="generateAudioReplyReturn"></param>
        void ParseMarks(IEnumerable<SpeechMark> marks, ref GenerateAudioReply generateAudioReplyReturn)
        {
            const double MILLISECONDS_IN_SECOND = 1000;
            int wordIndex = 0;
            double lastTime = 0;
            foreach (SpeechMark mark in marks)
            {
                lastTime = mark.time / MILLISECONDS_IN_SECOND;

                if (mark.type == "viseme")
                {
                    if (m_IPAtoFacefxMap.ContainsKey(mark.value))
                    {
                        IPAtoFacefxMap map = m_IPAtoFacefxMap[mark.value];
                        for (int i = 0; i < map.facefxVisemes.Length; i++)
                        {
                            generateAudioReplyReturn.VisemeList.Add(new GenerateAudioReplyViseme(map.facefxVisemes[i], mark.time / MILLISECONDS_IN_SECOND, map.amounts[i]));
                        }
                    }
                    else
                    {
                        Console.WriteLine(string.Format("Failed to map IPA {0} to facefx. Disgarding", mark.value));
                    }
                }
                else if (mark.type == "word")
                {
                    double time = mark.time / MILLISECONDS_IN_SECOND;
                    generateAudioReplyReturn.MarkList.Add(new KeyValuePairS<string, double>("T" + wordIndex, time));
                    generateAudioReplyReturn.WordBreakList.Add(new KeyValuePairS<double, double>(time, 0));

                    int prevWorldIndex = wordIndex - 1;
                    if (prevWorldIndex >= 0)
                    {
                        ChangeEndWordTiming(generateAudioReplyReturn.WordBreakList, prevWorldIndex, time);
                    }
                    wordIndex += 1;
                }
            }

            if (generateAudioReplyReturn.WordBreakList.Count > 0)
            {
                generateAudioReplyReturn.MarkList.Add(new KeyValuePairS<string, double>("T" + wordIndex, lastTime));
                ChangeEndWordTiming(generateAudioReplyReturn.WordBreakList, generateAudioReplyReturn.WordBreakList.Count -1, lastTime);
            }
            
        }

        void ChangeEndWordTiming(List<KeyValuePairS<double, double>> wordTimings, int index, double endTime)
        {
            KeyValuePairS<double, double> pair = wordTimings[index];
            pair.Value = endTime;
            wordTimings[index] = pair;
        }

        public static void CopyStream(Stream input, Stream output)
        {
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = input.Read(buffer, 0, buffer.Length)) > 0)
            {
                output.Write(buffer, 0, len);
            }
        }

        public string[] GetVoices()
        {
            List<string> voices = new List<string>();
            if (DoesClientExist())
            {
                DescribeVoicesRequest voiceRequest = new DescribeVoicesRequest();
                DescribeVoicesResponse response = client.DescribeVoices(voiceRequest);
                if (response != null && response.Voices != null)
                {
                    foreach (Voice voice in response.Voices)
                    {
                        string name = voice.Name;
                        name = "Polly_" + name;
                        name = name.Replace(" ", "|");
                        voices.Add(name);
                    }
                }
                else
                {
                    Console.WriteLine("Failed to find AWS Polly voices");
                }
            }
            else
            {
                Console.WriteLine("Failed to find AWS Polly voices because it wasn't initialized");
            }
            
            return voices.ToArray();
        }

        public bool Init(string visemeMapping)
        {
            CreateClient();
            SetVisemeMapping(visemeMapping);
            return client != null;
        }

        public void SetVisemeMapping(string visemeMapping)
        {
            pvMapInit(visemeMapping);
        }

        /// <summary>
        /// Initialize all phoneme to viseme mappings
        /// </summary>
        private void pvMapInit(string visemeMapping)
        {
            visemeMappingType = visemeMapping;

            try
            {
                //facefxMappingDoc = new XmlDocument();
                //facefxMappingDoc.LoadXml(facefxMapping);
                ParseIPAMap(/*facefxMappingDoc*/);
            }
            catch (Exception e)
            {
                Console.WriteLine("error loading the xml string for facefx visemes:" + e.ToString() + "\n");
            }
            
        }

        /// <summary>
        /// Maps IPA phone to facefx viseme
        /// </summary>
        /// <param name="doc"></param>
        void ParseIPAMap(/*XmlDocument doc*/)
        {

            m_IPAtoFacefxMap.Add("p", new IPAtoFacefxMap("p", new string[] { "BMP" }, new float[] { 1 }));
            m_IPAtoFacefxMap.Add("t", new IPAtoFacefxMap("t", new string[] { "open", "tRoof" }, new float[] { 0.6f, 1 }));
            m_IPAtoFacefxMap.Add("S", new IPAtoFacefxMap("S", new string[] { "ShCh" }, new float[] { 1 }));
            m_IPAtoFacefxMap.Add("T", new IPAtoFacefxMap("T", new string[] { "open", "tTeeth" }, new float[] { 0.4f, 0.9f }));
            m_IPAtoFacefxMap.Add("f", new IPAtoFacefxMap("f", new string[] { "FV" }, new float[] { 1 }));
            m_IPAtoFacefxMap.Add("k", new IPAtoFacefxMap("k", new string[] { "open", "tBack" }, new float[] { 0.3f, 1 }));
            m_IPAtoFacefxMap.Add("i", new IPAtoFacefxMap("i", new string[] { "open", "wide" }, new float[] { 0.4f, 0.1f }));
            m_IPAtoFacefxMap.Add("r", new IPAtoFacefxMap("r", new string[] { "ShCh", "W" }, new float[] { 0.2f, 0.1f }));
            m_IPAtoFacefxMap.Add("s", new IPAtoFacefxMap("s", new string[] { "open", "tRoof" }, new float[] { 0.15f, 0.8f }));
            m_IPAtoFacefxMap.Add("u", new IPAtoFacefxMap("u", new string[] { "W" }, new float[] { 0.9f }));
            m_IPAtoFacefxMap.Add("@", new IPAtoFacefxMap("@", new string[] { "open", "wide" }, new float[] { 0.35f, 0.1f }));
            m_IPAtoFacefxMap.Add("a", new IPAtoFacefxMap("a", new string[] { "open", "wide" }, new float[] { 0.7f, 0.15f }));
            m_IPAtoFacefxMap.Add("e", new IPAtoFacefxMap("e", new string[] { "open", "wide" }, new float[] { 0.4f, 0.25f }));
            m_IPAtoFacefxMap.Add("E", new IPAtoFacefxMap("E", new string[] { "ShCh", "BMP" }, new float[] { 0.3f, 0.1f }));
            m_IPAtoFacefxMap.Add("O", new IPAtoFacefxMap("O", new string[] { "open" }, new float[] { 0.7f }));
            m_IPAtoFacefxMap.Add("o", new IPAtoFacefxMap("o", new string[] { "open", "W" }, new float[] { 0.25f, 0.45f }));
            /*XmlNodeList entries = doc.GetElementsByTagName("entry");

            for (int i = 0; i < entries.Count; ++i)
            {
                
                string viseme = entries[i].Attributes["target"].InnerText;
                string articulation = entries[i].Attributes["amount"].InnerText;
                string phoneme = entries[i].Attributes["phoneme"].InnerText;

                if (!m_IPAtoFacefxMap.ContainsKey(phoneme))
                {
                    m_IPAtoFacefxMap.Add(phoneme, new IPAtoFacefxMap(phoneme, viseme, float.Parse(articulation)));
                }
            }*/
        }

        void CreateClient()
        {
            if (!DoesClientExist())
            {
                client = new AmazonPollyClient("ABCDEFGHIJKLMNOPQRST", "01234567890abcdefghijklmnopqrstuvwxyz012", Amazon.RegionEndpoint.USEast1);
            }
        }

        bool DoesClientExist() { return client != null ; } // TODO: check connection
    }
}
