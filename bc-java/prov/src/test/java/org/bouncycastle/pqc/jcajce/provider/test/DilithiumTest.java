package org.bouncycastle.pqc.jcajce.provider.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import junit.framework.TestCase;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.interfaces.DilithiumKey;
import org.bouncycastle.pqc.jcajce.interfaces.DilithiumPrivateKey;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

/**
 * Dilithum now in BC provider
 */
public class DilithiumTest
    extends TestCase
{
    byte[] msg = Strings.toByteArray("Hello World!");

    public void setUp()
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void testPrivateKeyRecovery()
            throws Exception
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BC");

        kpg.initialize(DilithiumParameterSpec.dilithium3, new DilithiumTest.RiggedRandom());

        KeyPair kp = kpg.generateKeyPair();

        KeyFactory kFact = KeyFactory.getInstance("Dilithium", "BC");

        DilithiumKey privKey = (DilithiumKey)kFact.generatePrivate(new PKCS8EncodedKeySpec(kp.getPrivate().getEncoded()));

        assertEquals(kp.getPrivate(), privKey);
        assertEquals(kp.getPrivate().getAlgorithm(), privKey.getAlgorithm());
        assertEquals(kp.getPrivate().hashCode(), privKey.hashCode());

        assertEquals(((DilithiumPrivateKey)kp.getPrivate()).getPublicKey(), ((DilithiumPrivateKey)privKey).getPublicKey());

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream oOut = new ObjectOutputStream(bOut);

        oOut.writeObject(privKey);

        oOut.close();

        ObjectInputStream oIn = new ObjectInputStream(new ByteArrayInputStream(bOut.toByteArray()));

        DilithiumKey privKey2 = (DilithiumKey)oIn.readObject();

        assertEquals(privKey, privKey2);

        assertEquals(kp.getPublic(), ((DilithiumPrivateKey)privKey2).getPublicKey());
        assertEquals(kp.getPrivate().getAlgorithm(), privKey2.getAlgorithm());
        assertEquals(kp.getPrivate().hashCode(), privKey2.hashCode());

        assertEquals(((DilithiumPrivateKey)privKey).getPublicKey(), ((DilithiumPrivateKey)privKey2).getPublicKey());
    }

    public void testPublicKeyRecovery()
            throws Exception
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BC");

        kpg.initialize(DilithiumParameterSpec.dilithium5, new DilithiumTest.RiggedRandom());

        KeyPair kp = kpg.generateKeyPair();

        KeyFactory kFact = KeyFactory.getInstance("Dilithium", "BC");

        DilithiumKey pubKey = (DilithiumKey)kFact.generatePublic(new X509EncodedKeySpec(kp.getPublic().getEncoded()));

        assertEquals(kp.getPublic(), pubKey);
        assertEquals(kp.getPublic().getAlgorithm(), pubKey.getAlgorithm());
        assertEquals(kp.getPublic().hashCode(), pubKey.hashCode());

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream oOut = new ObjectOutputStream(bOut);

        oOut.writeObject(pubKey);

        oOut.close();

        ObjectInputStream oIn = new ObjectInputStream(new ByteArrayInputStream(bOut.toByteArray()));

        DilithiumKey pubKey2 = (DilithiumKey)oIn.readObject();

        assertEquals(pubKey, pubKey2);
        assertEquals(pubKey.getAlgorithm(), pubKey2.getAlgorithm());
        assertEquals(pubKey.hashCode(), pubKey2.hashCode());
    }

    public void testRestrictedSignature()
        throws Exception
    {
        doTestRestrictedSignature("DILITHIUM2", DilithiumParameterSpec.dilithium2, DilithiumParameterSpec.dilithium5);
        doTestRestrictedSignature("DILITHIUM3", DilithiumParameterSpec.dilithium3, DilithiumParameterSpec.dilithium5);
        doTestRestrictedSignature("DILITHIUM5", DilithiumParameterSpec.dilithium5, DilithiumParameterSpec.dilithium2);
    }

    private void doTestRestrictedSignature(String sigName, DilithiumParameterSpec spec, DilithiumParameterSpec altSpec)
        throws Exception
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BC");

        kpg.initialize(spec, new SecureRandom());

        KeyPair kp = kpg.generateKeyPair();

        Signature sig = Signature.getInstance(sigName, "BC");

        sig.initSign(kp.getPrivate(), new SecureRandom());

        sig.update(msg, 0, msg.length);

        byte[] s = sig.sign();

        sig = Signature.getInstance(sigName, "BC");

        assertEquals(sigName, sig.getAlgorithm());

        sig.initVerify(kp.getPublic());

        sig.update(msg, 0, msg.length);

        assertTrue(sig.verify(s));

        kpg = KeyPairGenerator.getInstance("Dilithium", "BC");

        kpg.initialize(altSpec, new SecureRandom());

        kp = kpg.generateKeyPair();

        try
        {
            sig.initVerify(kp.getPublic());
            fail("no exception");
        }
        catch (InvalidKeyException e)
        {
            assertEquals("signature configured for " + spec.getName(), e.getMessage());
        }
    }

    public void testRestrictedKeyPairGen()
        throws Exception
    {
        doTestRestrictedKeyPairGen(DilithiumParameterSpec.dilithium2, DilithiumParameterSpec.dilithium5);
        doTestRestrictedKeyPairGen(DilithiumParameterSpec.dilithium3, DilithiumParameterSpec.dilithium5);
        doTestRestrictedKeyPairGen(DilithiumParameterSpec.dilithium5, DilithiumParameterSpec.dilithium2);
    }

    private void doTestRestrictedKeyPairGen(DilithiumParameterSpec spec, DilithiumParameterSpec altSpec)
        throws Exception
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(spec.getName(), "BC");

        kpg.initialize(spec, new SecureRandom());

        KeyPair kp = kpg.generateKeyPair();

        assertEquals(spec.getName(), kpg.getAlgorithm());
        assertEquals(spec.getName(), kp.getPublic().getAlgorithm());
        assertEquals(spec.getName(), kp.getPrivate().getAlgorithm());

        kpg = KeyPairGenerator.getInstance(spec.getName(), "BC");

        try
        {
            kpg.initialize(altSpec, new SecureRandom());
            fail("no exception");
        }
        catch (InvalidAlgorithmParameterException e)
        {
            assertEquals("key pair generator locked to " + spec.getName(), e.getMessage());
        }
    }

    public void testDilithiumRandomSig()
            throws Exception
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BC");

        kpg.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());

        KeyPair kp = kpg.generateKeyPair();

        Signature sig = Signature.getInstance("Dilithium", "BC");

        sig.initSign(kp.getPrivate(), new SecureRandom());

        sig.update(msg, 0, msg.length);

        byte[] s = sig.sign();

        sig = Signature.getInstance("Dilithium", "BC");

        sig.initVerify(kp.getPublic());

        sig.update(msg, 0, msg.length);

        assertTrue(sig.verify(s));
    }

    /**
count = 0
seed = 061550234D158C5EC95595FE04EF7A25767F2E24CC2BC479D09D86DC9ABCFDE7056A8C266F9EF97ED08541DBD2E1FFA1
mlen = 33
msg = D81C4D8D734FCBFBEADE3D3F8A039FAA2A2C9957E835AD55B22E75BF57BB556AC8
pk = 1C0EE1111B08003F28E65E8B3BDEB037CF8F221DFCDAF5950EDB38D506D85BEF6177E3DE0D4F1EF5847735947B56D08E841DB2444FA2B729ADEB1417CA7ADF42A1490C5A097F002760C1FC419BE8325AAD0197C52CED80D3DF18E7774265B289912CECA1BE3A90D8A4FDE65C84C610864E47DEECAE3EEA4430B9909559408D11A6ABDB7DB9336DF7F96EAB4864A6579791265FA56C348CB7D2DDC90E133A95C3F6B13601429F5408BD999AA479C1018159550EC55A113C493BE648F4E036DD4F8C809E036B4FBB918C2C484AD8E1747AE05585AB433FDF461AF03C25A773700721AA05F7379FE7F5ED96175D4021076E7F52B60308EFF5D42BA6E093B3D0815EB3496646E49230A9B35C8D41900C2BB8D3B446A23127F7E096D85A1C794AD4C89277904FC6BFEC57B1CDD80DF9955030FDCA741AFBDAC827B13CCD5403588AF4644003C2265DFA4D419DBCCD2064892386518BE9D51C16498275EBECF5CDC7A820F2C29314AC4A6F08B2252AD3CFB199AA42FE0B4FB571975C1020D949E194EE1EAD937BFB550BB3BA8E357A029C29F077554602E1CA2F2289CB9169941C3AAFDB8E58C7F2AC77291FB4147C65F6B031D3EBA42F2ACFD9448A5BC22B476E07CCCEDA2306C554EC9B7AB655F1D7318C2B7E67D5F69BEDF56000FDA98986B5AB1B3A22D8DFD6681697B23A55C96E8710F3F98C044FB15F606313EE56C0F1F5CA0F512E08484FCB358E6E528FFA89F8A866CCFF3C0C5813147EC59AF0470C4AAD0141D34F101DA2E5E1BD52D0D4C9B13B3E3D87D1586105796754E7978CA1C68A7D85DF112B7AB921B359A9F03CBD27A7EAC87A9A80B0B26B4C9657ED85AD7FA2616AB345EB8226F69FC0F48183FF574BCD767B5676413ADB12EA2150A0E97683EE54243C25B7EA8A718606F86993D8D0DACE834ED341EEB724FE3D5FF0BC8B8A7B8104BA269D34133A4CF8300A2D688496B59B6FCBC61AE96062EA1D8E5B410C5671F424417ED693329CD983001FFCD10023D598859FB7AD5FD263547117100690C6CE7438956E6CC57F1B5DE53BB0DC72CE9B6DEAA85789599A70F0051F1A0E25E86D888B00DF36BDBC93EF7217C45ACE11C0790D70E9953E5B417BA2FD9A4CAF82F1FCE6F45F53E215B8355EF61D891DF1C794231C162DD24164B534A9D48467CDC323624C2F95D4402FF9D66AB1191A8124144AFA35D4E31DC86CAA797C31F68B85854CD959C4FAC5EC53B3B56D374B888A9E979A6576B6345EC8522C9606990281BF3EF7C5945D10FD21A2A1D2E5404C5CF21220641391B98BCF825398305B56E58B611FE5253203E3DF0D22466A73B3F0FBE43B9A62928091898B8A0E5B269DB586B0E4DDEF50D682A12D2C1BE824149AA254C6381BB412D77C3F9AA902B688C81715A59C839558556D35ED4FC83B4AB18181F40F73DCD76860D8D8BF94520237C2AC0E463BA09E3C9782380DC07FE4FCBA340CC2003439FD2314610638070D6C9EEA0A70BAE83B5D5D3C5D3FDE26DD01606C8C520158E7E5104020F248CEAA666457C10AEBF068F8A3BD5CE7B52C6AF0ABD5944AF1AD4752C9113976083C03B6C34E1D47ED69644CAD782C2F7D05F8A148961D965FA2E1723A8DDEBC22A90CD783DD1F4DB38FB9AE5A6714B3D946781643D317B7DD79381CF789A9588BB3E193B92A0B60D6B07D047F6984B0609EC57543C394CA8D5E5BCC2A731A79618BD1E2E0DA8704AF98F20F5F8F5452DDF646B95B341DD7F0D2CC1FA15BD9895CD5B65AA1CB94B5E2E788FDA9825B656639193D98328154A4F2C35495A38B6EA0D2FFAAA35DF92C203C7F31CBBCA7BD03C3C2302190CECD161FD49237E4F839E3F3
sk = 1C0EE1111B08003F28E65E8B3BDEB037CF8F221DFCDAF5950EDB38D506D85BEF394D1695059DFF40AE256C5D5EDABFB69F5F40F37A588F50532CA408A8168AB187D0AD11522110931494BF2CAEAE36979711BC585B32F08C78496F379D604D5321C8C62B59EDC23AE1FC7742135918E01B02E411630E26E675400D5AD2C776FCC0A6711A966C11312AD9A821D8086542A600A4B42C1940720242628106210A43852331709308108B188C022492C1B28412C4218B042181C8610248059C9201C0348819326C582046891868A2C28D82346A1C094200A28CE3A6491C112CC24812E0902191985062C084622451CA062C64240E1BB3312496854B4606DB2668C38268441046C9B6211404811445502442084422710B92459AA0811A91709C241003957004C504C82692D29200C0B260C0A26809190AA2300E188969E0008DD84862DA14712018051907440412409B1240118010D142819928508B1091022464A0206D1246211C838C1B4769010690CC062481846920982C24120521B15041360298446ED1A63111056AD3A840CAA84C62B00003134A53344614194004C54CE306695AB08961168ECB10808B168ED990640B94602483851AB30454262251B8251C424A0B814842C4445A102023808409B7254CC64814854D19380E601651D8326A0A918908C170E0964D18468C01328D91C4054A0061230868A2104210A8611306218A248E620689C9B24508278451200D980466DC42054424852426282221612016090BA62C0A1144E0928158480D422210A006098B246E81288CC0248090308D8436404CA68450042494B68DA2926D18B344A00085E3B805140504A4C290842281C3262D0B2066CC903198382810166CC13445C0102224C688034632D840901C20680415289A188144988D9C206E9C302CC1B820614221080310A0C28C58128553204C0330814CA48D44C08D51404C1CA72C440865A03840DA20808106858C260DE2A88C9C4411594228C42604441426A1426408C0851101869B483199B20C80464459A88C0042089882900AB54562244812960544124600C88813A061E1284D0AB9914B962099B84400314E98128500B60183A00D14150E1881101901224A06681A498DE1A28411C63121262591A06D030524A1B6089444724334125BB42041B650D0888D0B074D1C94644C208E8B8808E0300944200549864D03134E19C9840937611A43684A80900204311C1742184080C8308EE1A241C33404A3282251247188D6FEF46712CA182872AB2919678AFF9D94E743E063A39E0C35CAF72A7F2EDA28E65858520D5D8467DE747CF340653B52C268F55413F5ADDC7D49011EC33EDD537423A84288869337AEA0781A124269071451722DB3BB8F2CE5B1552F83D2AF07F25613918A9F4E6F1257603888E589308CA5F95F07143D23BAAE17520B36B6E0E94FAF6845EB2131AEC383E63BC8644EE5F1ACCBA82F9211E57AFCBF509C1131A37466BC91B357DCBBBC14CCC319C4CC6AC75FCDC82C6596D07770C8277AD370B192A0B4E05F812E0E265D2912AA29F03FC9F72DFA69C9B1291A3FC583642B235F6991A954788347F60A0328C48ECEE51BA02DFF323ABD911667CB14549B618F1C5D250CAC9E35E071601992FBEC0BAE6F74213081404744D12F2A0E04BDB265E0924CADA40D1FA1F38ACA4606BFD4575712B8260A456FDDEEEFE7CA259BCDA97B9B939A5FD2889C9B49FB7D4E3553DEA61B3339BD0E6B16BF3BB227103BF9202E72DC502E28F7CE1559A4631F372520324E4EBA07545F78BF4D94B0E5B8BF51B8F176533D5CFEA5232F283A47605FA65DDB17C891C251011C4E98EEB6EB00CB65BA31C8F025C87A9FE02DBC10C5D83A065EBA5D7B2A19D5A1CB2C160AE166E867F2AF8C7D49D63FB83A614957FC0A3B5A5C74990E9A2B02120C7E6DE37E155FB472F50F0A45E47CF5F9D7A4C82982C9DC86AE877C3FD1885943E439FB003C7A9A42F71B4FF6F0A28B140CBDBA6E71B13AC31B23DE9EAB7837E15A69F833EB7B56A71D8BC2CAF1F2A31C345BD5F46EE013A7C689372337191DAA800C0AC6C46C9FF688B1A01347F257C474AA3D97C1D63A8C00E0A37B681673F57C1C9C8FCCD46F174C74A29D84CEB71F7E6B2F8CD2B089ED43F7C96DAE81A223418C20B16F1DF3D1A978AE28F6DF35EC559D04D20EC74B224AEA31A289B015B069E9CBBBF7CF6DE94CFB2A96E4AE3462C96003CDDA87DB561AF2CE3C0BA1D90413FDCE3CCF4390C02C1CB9F654F4820EC33015457D4A629FBF39419CAB7642D6885E103FCE0D4206CCE7C12C6FC44FA33AD0864C3371A7CBE820E3B371B656A38F2E7FF18FE4A50C8AB3F85D783FB57835CED8490B84EE0D99AF0D64C483CEB6366FF54F8AC8A40DB1AFA573A4FB326C74F0236ECEF3DA7120665CCE05DD654B5071723A8348E7CD7793513819B61CB64E1328E8B22E7664BD6B41B5710D19EA8809D4450850E907DFC4D0B75F588CECE962E9E0937CE1402446A4D2891A46E6617FB29D4FCD712606F7819ECA60F7E0D5B19E7FFB57C73C16FFEEB90038410CB9FCBB5E9D51EB3EB6297E9FF6AB7088FE2D9B237BC24CF7F8290118A5E0E00A0B903FB6375C848176CD0A8C8875CC59199CDA11A87A78F65CC404330B087571FD0633E27129FDAB5A8A1F793E52412B0083FD5C74DB3CF60C2543CE7C91B2800E40203F8D99FE5FDE5B108E7EDC80EBB9BB34986EC5C5A8F580E75752907FF0F294C866C2CF1F362E840B6881BD43219201781C63B0039A95BCFB4A0FECE569DF00523CE9C084B022B3B022242E28419796ACF0A0C995F948DBFFFD30D77ED105A3C9943C406B305BC81A6A248A291548F2A67F438D966A57D53F4B7BE15354E581BE16F7AD64D164E85787DF5849C810AFC28D06482F441B5FDE3DB2ED36DD25AA6664D4D43FFA32EDA25689C9F4A5D514FC66231C5401520922524438EF1DC78D693C9718DEBBD243312674C899F18910E389C8EBE505824BCC42CD4A9ACE193768220219011F3B1F335427BFF9E8BDED5C08711A09C2B71CB964C56A8393BFD2B56E9B6B2F513E682587DC1B8ED196066326871025628036700063176D345DE384E182D6C417A32AB11095EF59BB4D171B9CF81D17AC42664DED933CCB722C69857FFC53C8E7F2474B0CB2DFF2DDC8A5C601C84A701981199BCCF74112A6EC062C4FEB601A028AF01032ADB6BD15D4C2B9550AA850AD62CCC3A3665D5212B12E0FD5C5326A1E5EB1F10D557D94605E8E3F356E08FF7FD884ED3C4205463594C9AF2F39E4B1274695234B54EECED93F460EDF1A13C2CB4B17D322F6F79FE16F0357C1C4739863E796791F8647FABF730AB00E0DA509706D94571740F61F7BAF366D2774C9B5B8C61DD6BE9819A6028B264BB2E4AEA54B56D4ECAB5B528CE0C0C0CCDB73023352CB00445BAB6F7467B4644D4361C464FAC6B5B137D32391021B475FCB5F31774FD8ECABDF65475F25574C65559CB331F41C0F498B74DD941C344C50D8E64F9578714A32561FAACEAF78148E6DA4B566826925714B17108AFDD546385A3CD454D5CAA16960916282A47C4315CE236BD9E3255C604EBDC39772DB5CE0B236
smlen = 2453
sm = 3D7F3A26A1A6DC133D036981F7406AE0858C74121BDA303DD5DA8D9ACB68409F1051C88C4B163C252DDB5E78E8EB867279A17289B34CD3BA4AA199AE56B28356EE49FF8304086E7CAA6B0DBA7EF60AD5ED9411A82FF9BE7D6177908977EF67CCD532A4723F125F4748B350C3948F2AC6C4F006CACB8C92CDC0941CDE2EFB4B732BF85954F4BA8417561403A863E0261A29D79987859976B4F8BDC7BC5EF215A07ED6004343CC7CFE79ECC7143AFD525CA35ADB5D603CAF97BD0A80104E4DE48FB41668F314415096E3547554D25FA09E9C14E60BD15A6DDCD0710A0FED464079229CA65A636E15D9215283767241FB6EED385B51416660F95AA8A619B55FA38B9A7CB710FBC0AD6237C72BECFB9D3182229E06A696B5E32B4B2EF2164349B54266BA9734EAD45387CA913507E3E75B49FEA7D3BD03A7EEE2EE8AFE048DD9E38686D5A1C5DB31A8FC960FD3575496CD301CDB952D8CF85792DEDF7FF6FA5BBF5101288EE80AFE1183B4A6689AE72E66B50393DC3345DF62BA2DCB999158FD8FD9A75AF95ED9C3EA325FEC21C5B611B267B938AE02580C72FB94E8910DBA88A32811B6FEE8A04355EBDEEDFAEC85F5FFDD6811FA4A3CC6323CDD93E6CE7F98688022401AF54288BF888B289F972FB98ECABF0D2C364344BBD2FFDAAE518A66370FF6BCA7D996B03BA3140890840E5EDD3EB98672D266F47A2E15255656CA978F14943BD40B1B21041173F6058391AA259D7E4F76C10DA3CF3AEE9B71A127A55DCB80AD822337C1D79C763CD7774A31A58743A4797D52DD3959A66BDB08338D007E2CA7CD19B0C553045C40D3E7AB0D318378799DD9A02B6C2B0C7C9B8DB986668598605163709193AC4DF5B19A5CE28BDD7CAD59AFF10FAA2220284DBE5D4C7FDF2792C559A6076865081D5F4513CFAE092458FD410E18BE1BC5F970660BB0C89C020079C121A1953C2AF9298A6342D1C47C413B4B3C35DD91358DEBE7DC109F35A3512514DBEBB544851709EC1A750550422F1C9FA40B50DE08DBFDE90593D229E01BD9F0756CBA1EBACB8CC2139D4CADC778BF937BD524E8845ECF964A04F7C43CD056F6A7A810C77C8B8FA73359CD1EB8670E1AF7F4BC247B7EC515C1BBA404B76635762D4E0EF451150C8A58437C06FD2C4154A00D63408F1EEE5D1B67F7F4893C158A765237C4FDB215CC0E3F4D60437AF43EF9AC575C0C6B85A93D5493DAB60961D55C4BEACE3A907597CCFC7C6EFB5453DCF83796AFD070322A650BDEA47B76DFF7756CEA567961830E7DC49B2A8923C59BECADD06435D6EFBC7F5307FDA057DAEB1C5B4F6E64D8E141A46090C9EF90D3816453F975C3C7158560DAFEE463148AC0E1E5351020F0A7C08A7C14C1AA9581C936EF845E011E82DE64FB4CB49DA4E3C8D079EF7DEEB41665C6ED43A4F161CBB795AC4FE1A67D6FE18CFB1A15BC02066A2598EFAA9FACC5BDD7257C68E309B2E2622D8C647A3D4656DEB71D414100049AA42C991F997F81A9B391449C4DAB874F9F309463A508E950501590FBC2ED4E80C2D63CE0DB72DE74D7CF9AAC845BE2502B89247D971EB5169A583677CC88C569067E726F9DDD1B49E80220F5B764CE4A32049E20C7FC2A573BFB911EB4AF50B9C2E1F5195AE76FC2F54D0BA33F2CDE2DB3084C5E5F25155D8D81082EAEF09C598A699373B5CCFD7DFB9ED2DDA4DD4681B073B24D6135D65A8ECB41CEB156B8D8F77A4DA1747239D0E7DE48441E90C62FB26DDB0E802DEEA997A6A2569885D0CBB2833A12D4BE92FFCB9AE3A3CFB01874C6A82427A7052ED0E6652DA9BA95280E24B65F8EAB174812011DD12D9062B1004C60DE85685D7D41FB5F04E9707E034A305B60145DF6686818CCA3457BA1DEEE0235D3B1D026F69A2AC556A1A93455F712C3A737BB4A30CE52F0204AB79F65B3E305EF89686D213B08AA538F4BA486C8709C8627C51DE86596D8EB035D807AFFC6F68D88E0B145DEABE8AAAEB411D085827E7CB47E3C568207FBEE7BA9568B414C0CADB05DA7D36F83037847A9F7233135F49FC14496485071CA5C5A0D1725C016E7482B6F9892D64FF76C6AF73330EE4C654654943F9966DAF3356C7ED8E4A0DD2F58B73B144D5FA286ADBE2A24776FEB78A4DD241EC3BF1DF78D5DDE6A48F8655F6FFC7D28543CA41F52F15CDC7CF092F48CEA91356D0EB1444A3290451033871F0006373F5A62CE9586ED95D3E361EFAD629B3A4D2C3643405DB4B7F837B7128C11E55C95C7F2AD80D507247485CFD4BE0A2EDDB877B3CE385C3ECFE71FF27ECA5D608AED19424037154B56BDB1A36908A09F1A50B1D89A21E6C0FB5C8AD21EC6DD997124DDF07F13BE0058583B070B2DF895223B7FB4A3A00343620436D6DA8114B779BC85CF9DE15C7EB6F26FD49F668FB33073554051B35DD0E5F62A66C47AF7CB3585A56E310FD7FB6336A5923AC5ACD57C72B348A1D8B42F52ABED61BFA58CAEBC9B20531F707C8A07813E66101282C30D86739AAD90790CFE9DE3C5D438318B696BB15BC2160A11FF03211CCEC77939F420BE1B6A8211565332779B86F18DA825F2F1174F4B9DF8C8F6F617648EE78C882688C4CE10C5FDE814B3917FF757AD7FE749129988CC43762002F89B24FADDC2D0926484C0C8B12B9944B177DB4A890E4826F72A4A0E19018781ECE90FB485443C7BE06C20C9DA7055F0AA87706B5A90DDB91834FAF746C2836C7C47496D8A0FD36FDAC574E924F7B514EDD7828215810D7370699C6C6C22D0AF97C289B49B99E4521EE8E8946FFCA48189C6653FA7F81D185E420D39B3BB34EDEC3D672AC0BA3890108400E25ED4CC877729F241E0D5BAED7EFC2BCAFC453BCEF9653C722D62C694420E509968F0BD3AADCCBD4E078B5E5B7E6A7833758167EC693E590982DCD54DCEA98BD3672E486E2A6F64A54366EEE3179636552CB832684B100D2AD75E91D86D7892DB3D7B3565953D35328973DAEF53955D8519B54A812550D8C11DD2A284845394A5395A7BC20F12450DC0C41769A2EDDA0A3256CFCFAF408F2405D31D795A8E1BC8C2A3E324595A96173575EF054F04214B0321A9A607E6DC6FA0EAF5CD0F26A3C1DEB15BDA4DB06E196AA145ED7ACD2E311B5C29AFFB26BC126E37FDBA4ECBE3A171CE7901161D62064B5F6B667D6011CEB90A19B8D05A4D2B1BFDDD8886F8F622F63D7E14D61B87A9177AF6EFCBA41E95BA35B2D0E330F9CAE832EA3CAA46DFBA1CB2D88D96B34F5DE2C12255AF89D0BC7FA9E5AAF1FC0A84CC3B6E9BDF25652A44F0DB30C4CEBE9298373CF54E73DA942D060F112B2F525364A3ACB0D2D3DEE2E7F908202D3E7C8FAEC5CFD7E0E3F506272A405D7486A0A7B2C7D9F3F8FC06222546647AAEB4CCFE00000000000000000000000000000000000000000000000000111E2D37D81C4D8D734FCBFBEADE3D3F8A039FAA2A2C9957E835AD55B22E75BF57BB556AC8
     * @throws Exception
     */
    public void testDilithiumKATSig()
            throws Exception
    {
        byte[] pubK = Hex.decode("1C0EE1111B08003F28E65E8B3BDEB037CF8F221DFCDAF5950EDB38D506D85BEF6177E3DE0D4F1EF5847735947B56D08E841DB2444FA2B729ADEB1417CA7ADF42A1490C5A097F002760C1FC419BE8325AAD0197C52CED80D3DF18E7774265B289912CECA1BE3A90D8A4FDE65C84C610864E47DEECAE3EEA4430B9909559408D11A6ABDB7DB9336DF7F96EAB4864A6579791265FA56C348CB7D2DDC90E133A95C3F6B13601429F5408BD999AA479C1018159550EC55A113C493BE648F4E036DD4F8C809E036B4FBB918C2C484AD8E1747AE05585AB433FDF461AF03C25A773700721AA05F7379FE7F5ED96175D4021076E7F52B60308EFF5D42BA6E093B3D0815EB3496646E49230A9B35C8D41900C2BB8D3B446A23127F7E096D85A1C794AD4C89277904FC6BFEC57B1CDD80DF9955030FDCA741AFBDAC827B13CCD5403588AF4644003C2265DFA4D419DBCCD2064892386518BE9D51C16498275EBECF5CDC7A820F2C29314AC4A6F08B2252AD3CFB199AA42FE0B4FB571975C1020D949E194EE1EAD937BFB550BB3BA8E357A029C29F077554602E1CA2F2289CB9169941C3AAFDB8E58C7F2AC77291FB4147C65F6B031D3EBA42F2ACFD9448A5BC22B476E07CCCEDA2306C554EC9B7AB655F1D7318C2B7E67D5F69BEDF56000FDA98986B5AB1B3A22D8DFD6681697B23A55C96E8710F3F98C044FB15F606313EE56C0F1F5CA0F512E08484FCB358E6E528FFA89F8A866CCFF3C0C5813147EC59AF0470C4AAD0141D34F101DA2E5E1BD52D0D4C9B13B3E3D87D1586105796754E7978CA1C68A7D85DF112B7AB921B359A9F03CBD27A7EAC87A9A80B0B26B4C9657ED85AD7FA2616AB345EB8226F69FC0F48183FF574BCD767B5676413ADB12EA2150A0E97683EE54243C25B7EA8A718606F86993D8D0DACE834ED341EEB724FE3D5FF0BC8B8A7B8104BA269D34133A4CF8300A2D688496B59B6FCBC61AE96062EA1D8E5B410C5671F424417ED693329CD983001FFCD10023D598859FB7AD5FD263547117100690C6CE7438956E6CC57F1B5DE53BB0DC72CE9B6DEAA85789599A70F0051F1A0E25E86D888B00DF36BDBC93EF7217C45ACE11C0790D70E9953E5B417BA2FD9A4CAF82F1FCE6F45F53E215B8355EF61D891DF1C794231C162DD24164B534A9D48467CDC323624C2F95D4402FF9D66AB1191A8124144AFA35D4E31DC86CAA797C31F68B85854CD959C4FAC5EC53B3B56D374B888A9E979A6576B6345EC8522C9606990281BF3EF7C5945D10FD21A2A1D2E5404C5CF21220641391B98BCF825398305B56E58B611FE5253203E3DF0D22466A73B3F0FBE43B9A62928091898B8A0E5B269DB586B0E4DDEF50D682A12D2C1BE824149AA254C6381BB412D77C3F9AA902B688C81715A59C839558556D35ED4FC83B4AB18181F40F73DCD76860D8D8BF94520237C2AC0E463BA09E3C9782380DC07FE4FCBA340CC2003439FD2314610638070D6C9EEA0A70BAE83B5D5D3C5D3FDE26DD01606C8C520158E7E5104020F248CEAA666457C10AEBF068F8A3BD5CE7B52C6AF0ABD5944AF1AD4752C9113976083C03B6C34E1D47ED69644CAD782C2F7D05F8A148961D965FA2E1723A8DDEBC22A90CD783DD1F4DB38FB9AE5A6714B3D946781643D317B7DD79381CF789A9588BB3E193B92A0B60D6B07D047F6984B0609EC57543C394CA8D5E5BCC2A731A79618BD1E2E0DA8704AF98F20F5F8F5452DDF646B95B341DD7F0D2CC1FA15BD9895CD5B65AA1CB94B5E2E788FDA9825B656639193D98328154A4F2C35495A38B6EA0D2FFAAA35DF92C203C7F31CBBCA7BD03C3C2302190CECD161FD49237E4F839E3F3");
        byte[] privK = Hex.decode("1C0EE1111B08003F28E65E8B3BDEB037CF8F221DFCDAF5950EDB38D506D85BEF394D1695059DFF40AE256C5D5EDABFB69F5F40F37A588F50532CA408A8168AB187D0AD11522110931494BF2CAEAE36979711BC585B32F08C78496F379D604D5321C8C62B59EDC23AE1FC7742135918E01B02E411630E26E675400D5AD2C776FCC0A6711A966C11312AD9A821D8086542A600A4B42C1940720242628106210A43852331709308108B188C022492C1B28412C4218B042181C8610248059C9201C0348819326C582046891868A2C28D82346A1C094200A28CE3A6491C112CC24812E0902191985062C084622451CA062C64240E1BB3312496854B4606DB2668C38268441046C9B6211404811445502442084422710B92459AA0811A91709C241003957004C504C82692D29200C0B260C0A26809190AA2300E188969E0008DD84862DA14712018051907440412409B1240118010D142819928508B1091022464A0206D1246211C838C1B4769010690CC062481846920982C24120521B15041360298446ED1A63111056AD3A840CAA84C62B00003134A53344614194004C54CE306695AB08961168ECB10808B168ED990640B94602483851AB30454262251B8251C424A0B814842C4445A102023808409B7254CC64814854D19380E601651D8326A0A918908C170E0964D18468C01328D91C4054A0061230868A2104210A8611306218A248E620689C9B24508278451200D980466DC42054424852426282221612016090BA62C0A1144E0928158480D422210A006098B246E81288CC0248090308D8436404CA68450042494B68DA2926D18B344A00085E3B805140504A4C290842281C3262D0B2066CC903198382810166CC13445C0102224C688034632D840901C20680415289A188144988D9C206E9C302CC1B820614221080310A0C28C58128553204C0330814CA48D44C08D51404C1CA72C440865A03840DA20808106858C260DE2A88C9C4411594228C42604441426A1426408C0851101869B483199B20C80464459A88C0042089882900AB54562244812960544124600C88813A061E1284D0AB9914B962099B84400314E98128500B60183A00D14150E1881101901224A06681A498DE1A28411C63121262591A06D030524A1B6089444724334125BB42041B650D0888D0B074D1C94644C208E8B8808E0300944200549864D03134E19C9840937611A43684A80900204311C1742184080C8308EE1A241C33404A3282251247188D6FEF46712CA182872AB2919678AFF9D94E743E063A39E0C35CAF72A7F2EDA28E65858520D5D8467DE747CF340653B52C268F55413F5ADDC7D49011EC33EDD537423A84288869337AEA0781A124269071451722DB3BB8F2CE5B1552F83D2AF07F25613918A9F4E6F1257603888E589308CA5F95F07143D23BAAE17520B36B6E0E94FAF6845EB2131AEC383E63BC8644EE5F1ACCBA82F9211E57AFCBF509C1131A37466BC91B357DCBBBC14CCC319C4CC6AC75FCDC82C6596D07770C8277AD370B192A0B4E05F812E0E265D2912AA29F03FC9F72DFA69C9B1291A3FC583642B235F6991A954788347F60A0328C48ECEE51BA02DFF323ABD911667CB14549B618F1C5D250CAC9E35E071601992FBEC0BAE6F74213081404744D12F2A0E04BDB265E0924CADA40D1FA1F38ACA4606BFD4575712B8260A456FDDEEEFE7CA259BCDA97B9B939A5FD2889C9B49FB7D4E3553DEA61B3339BD0E6B16BF3BB227103BF9202E72DC502E28F7CE1559A4631F372520324E4EBA07545F78BF4D94B0E5B8BF51B8F176533D5CFEA5232F283A47605FA65DDB17C891C251011C4E98EEB6EB00CB65BA31C8F025C87A9FE02DBC10C5D83A065EBA5D7B2A19D5A1CB2C160AE166E867F2AF8C7D49D63FB83A614957FC0A3B5A5C74990E9A2B02120C7E6DE37E155FB472F50F0A45E47CF5F9D7A4C82982C9DC86AE877C3FD1885943E439FB003C7A9A42F71B4FF6F0A28B140CBDBA6E71B13AC31B23DE9EAB7837E15A69F833EB7B56A71D8BC2CAF1F2A31C345BD5F46EE013A7C689372337191DAA800C0AC6C46C9FF688B1A01347F257C474AA3D97C1D63A8C00E0A37B681673F57C1C9C8FCCD46F174C74A29D84CEB71F7E6B2F8CD2B089ED43F7C96DAE81A223418C20B16F1DF3D1A978AE28F6DF35EC559D04D20EC74B224AEA31A289B015B069E9CBBBF7CF6DE94CFB2A96E4AE3462C96003CDDA87DB561AF2CE3C0BA1D90413FDCE3CCF4390C02C1CB9F654F4820EC33015457D4A629FBF39419CAB7642D6885E103FCE0D4206CCE7C12C6FC44FA33AD0864C3371A7CBE820E3B371B656A38F2E7FF18FE4A50C8AB3F85D783FB57835CED8490B84EE0D99AF0D64C483CEB6366FF54F8AC8A40DB1AFA573A4FB326C74F0236ECEF3DA7120665CCE05DD654B5071723A8348E7CD7793513819B61CB64E1328E8B22E7664BD6B41B5710D19EA8809D4450850E907DFC4D0B75F588CECE962E9E0937CE1402446A4D2891A46E6617FB29D4FCD712606F7819ECA60F7E0D5B19E7FFB57C73C16FFEEB90038410CB9FCBB5E9D51EB3EB6297E9FF6AB7088FE2D9B237BC24CF7F8290118A5E0E00A0B903FB6375C848176CD0A8C8875CC59199CDA11A87A78F65CC404330B087571FD0633E27129FDAB5A8A1F793E52412B0083FD5C74DB3CF60C2543CE7C91B2800E40203F8D99FE5FDE5B108E7EDC80EBB9BB34986EC5C5A8F580E75752907FF0F294C866C2CF1F362E840B6881BD43219201781C63B0039A95BCFB4A0FECE569DF00523CE9C084B022B3B022242E28419796ACF0A0C995F948DBFFFD30D77ED105A3C9943C406B305BC81A6A248A291548F2A67F438D966A57D53F4B7BE15354E581BE16F7AD64D164E85787DF5849C810AFC28D06482F441B5FDE3DB2ED36DD25AA6664D4D43FFA32EDA25689C9F4A5D514FC66231C5401520922524438EF1DC78D693C9718DEBBD243312674C899F18910E389C8EBE505824BCC42CD4A9ACE193768220219011F3B1F335427BFF9E8BDED5C08711A09C2B71CB964C56A8393BFD2B56E9B6B2F513E682587DC1B8ED196066326871025628036700063176D345DE384E182D6C417A32AB11095EF59BB4D171B9CF81D17AC42664DED933CCB722C69857FFC53C8E7F2474B0CB2DFF2DDC8A5C601C84A701981199BCCF74112A6EC062C4FEB601A028AF01032ADB6BD15D4C2B9550AA850AD62CCC3A3665D5212B12E0FD5C5326A1E5EB1F10D557D94605E8E3F356E08FF7FD884ED3C4205463594C9AF2F39E4B1274695234B54EECED93F460EDF1A13C2CB4B17D322F6F79FE16F0357C1C4739863E796791F8647FABF730AB00E0DA509706D94571740F61F7BAF366D2774C9B5B8C61DD6BE9819A6028B264BB2E4AEA54B56D4ECAB5B528CE0C0C0CCDB73023352CB00445BAB6F7467B4644D4361C464FAC6B5B137D32391021B475FCB5F31774FD8ECABDF65475F25574C65559CB331F41C0F498B74DD941C344C50D8E64F9578714A32561FAACEAF78148E6DA4B566826925714B17108AFDD546385A3CD454D5CAA16960916282A47C4315CE236BD9E3255C604EBDC39772DB5CE0B236");
        byte[] msg = Hex.decode("D81C4D8D734FCBFBEADE3D3F8A039FAA2A2C9957E835AD55B22E75BF57BB556AC8");
        byte[] s = Hex.decode("3D7F3A26A1A6DC133D036981F7406AE0858C74121BDA303DD5DA8D9ACB68409F1051C88C4B163C252DDB5E78E8EB867279A17289B34CD3BA4AA199AE56B28356EE49FF8304086E7CAA6B0DBA7EF60AD5ED9411A82FF9BE7D6177908977EF67CCD532A4723F125F4748B350C3948F2AC6C4F006CACB8C92CDC0941CDE2EFB4B732BF85954F4BA8417561403A863E0261A29D79987859976B4F8BDC7BC5EF215A07ED6004343CC7CFE79ECC7143AFD525CA35ADB5D603CAF97BD0A80104E4DE48FB41668F314415096E3547554D25FA09E9C14E60BD15A6DDCD0710A0FED464079229CA65A636E15D9215283767241FB6EED385B51416660F95AA8A619B55FA38B9A7CB710FBC0AD6237C72BECFB9D3182229E06A696B5E32B4B2EF2164349B54266BA9734EAD45387CA913507E3E75B49FEA7D3BD03A7EEE2EE8AFE048DD9E38686D5A1C5DB31A8FC960FD3575496CD301CDB952D8CF85792DEDF7FF6FA5BBF5101288EE80AFE1183B4A6689AE72E66B50393DC3345DF62BA2DCB999158FD8FD9A75AF95ED9C3EA325FEC21C5B611B267B938AE02580C72FB94E8910DBA88A32811B6FEE8A04355EBDEEDFAEC85F5FFDD6811FA4A3CC6323CDD93E6CE7F98688022401AF54288BF888B289F972FB98ECABF0D2C364344BBD2FFDAAE518A66370FF6BCA7D996B03BA3140890840E5EDD3EB98672D266F47A2E15255656CA978F14943BD40B1B21041173F6058391AA259D7E4F76C10DA3CF3AEE9B71A127A55DCB80AD822337C1D79C763CD7774A31A58743A4797D52DD3959A66BDB08338D007E2CA7CD19B0C553045C40D3E7AB0D318378799DD9A02B6C2B0C7C9B8DB986668598605163709193AC4DF5B19A5CE28BDD7CAD59AFF10FAA2220284DBE5D4C7FDF2792C559A6076865081D5F4513CFAE092458FD410E18BE1BC5F970660BB0C89C020079C121A1953C2AF9298A6342D1C47C413B4B3C35DD91358DEBE7DC109F35A3512514DBEBB544851709EC1A750550422F1C9FA40B50DE08DBFDE90593D229E01BD9F0756CBA1EBACB8CC2139D4CADC778BF937BD524E8845ECF964A04F7C43CD056F6A7A810C77C8B8FA73359CD1EB8670E1AF7F4BC247B7EC515C1BBA404B76635762D4E0EF451150C8A58437C06FD2C4154A00D63408F1EEE5D1B67F7F4893C158A765237C4FDB215CC0E3F4D60437AF43EF9AC575C0C6B85A93D5493DAB60961D55C4BEACE3A907597CCFC7C6EFB5453DCF83796AFD070322A650BDEA47B76DFF7756CEA567961830E7DC49B2A8923C59BECADD06435D6EFBC7F5307FDA057DAEB1C5B4F6E64D8E141A46090C9EF90D3816453F975C3C7158560DAFEE463148AC0E1E5351020F0A7C08A7C14C1AA9581C936EF845E011E82DE64FB4CB49DA4E3C8D079EF7DEEB41665C6ED43A4F161CBB795AC4FE1A67D6FE18CFB1A15BC02066A2598EFAA9FACC5BDD7257C68E309B2E2622D8C647A3D4656DEB71D414100049AA42C991F997F81A9B391449C4DAB874F9F309463A508E950501590FBC2ED4E80C2D63CE0DB72DE74D7CF9AAC845BE2502B89247D971EB5169A583677CC88C569067E726F9DDD1B49E80220F5B764CE4A32049E20C7FC2A573BFB911EB4AF50B9C2E1F5195AE76FC2F54D0BA33F2CDE2DB3084C5E5F25155D8D81082EAEF09C598A699373B5CCFD7DFB9ED2DDA4DD4681B073B24D6135D65A8ECB41CEB156B8D8F77A4DA1747239D0E7DE48441E90C62FB26DDB0E802DEEA997A6A2569885D0CBB2833A12D4BE92FFCB9AE3A3CFB01874C6A82427A7052ED0E6652DA9BA95280E24B65F8EAB174812011DD12D9062B1004C60DE85685D7D41FB5F04E9707E034A305B60145DF6686818CCA3457BA1DEEE0235D3B1D026F69A2AC556A1A93455F712C3A737BB4A30CE52F0204AB79F65B3E305EF89686D213B08AA538F4BA486C8709C8627C51DE86596D8EB035D807AFFC6F68D88E0B145DEABE8AAAEB411D085827E7CB47E3C568207FBEE7BA9568B414C0CADB05DA7D36F83037847A9F7233135F49FC14496485071CA5C5A0D1725C016E7482B6F9892D64FF76C6AF73330EE4C654654943F9966DAF3356C7ED8E4A0DD2F58B73B144D5FA286ADBE2A24776FEB78A4DD241EC3BF1DF78D5DDE6A48F8655F6FFC7D28543CA41F52F15CDC7CF092F48CEA91356D0EB1444A3290451033871F0006373F5A62CE9586ED95D3E361EFAD629B3A4D2C3643405DB4B7F837B7128C11E55C95C7F2AD80D507247485CFD4BE0A2EDDB877B3CE385C3ECFE71FF27ECA5D608AED19424037154B56BDB1A36908A09F1A50B1D89A21E6C0FB5C8AD21EC6DD997124DDF07F13BE0058583B070B2DF895223B7FB4A3A00343620436D6DA8114B779BC85CF9DE15C7EB6F26FD49F668FB33073554051B35DD0E5F62A66C47AF7CB3585A56E310FD7FB6336A5923AC5ACD57C72B348A1D8B42F52ABED61BFA58CAEBC9B20531F707C8A07813E66101282C30D86739AAD90790CFE9DE3C5D438318B696BB15BC2160A11FF03211CCEC77939F420BE1B6A8211565332779B86F18DA825F2F1174F4B9DF8C8F6F617648EE78C882688C4CE10C5FDE814B3917FF757AD7FE749129988CC43762002F89B24FADDC2D0926484C0C8B12B9944B177DB4A890E4826F72A4A0E19018781ECE90FB485443C7BE06C20C9DA7055F0AA87706B5A90DDB91834FAF746C2836C7C47496D8A0FD36FDAC574E924F7B514EDD7828215810D7370699C6C6C22D0AF97C289B49B99E4521EE8E8946FFCA48189C6653FA7F81D185E420D39B3BB34EDEC3D672AC0BA3890108400E25ED4CC877729F241E0D5BAED7EFC2BCAFC453BCEF9653C722D62C694420E509968F0BD3AADCCBD4E078B5E5B7E6A7833758167EC693E590982DCD54DCEA98BD3672E486E2A6F64A54366EEE3179636552CB832684B100D2AD75E91D86D7892DB3D7B3565953D35328973DAEF53955D8519B54A812550D8C11DD2A284845394A5395A7BC20F12450DC0C41769A2EDDA0A3256CFCFAF408F2405D31D795A8E1BC8C2A3E324595A96173575EF054F04214B0321A9A607E6DC6FA0EAF5CD0F26A3C1DEB15BDA4DB06E196AA145ED7ACD2E311B5C29AFFB26BC126E37FDBA4ECBE3A171CE7901161D62064B5F6B667D6011CEB90A19B8D05A4D2B1BFDDD8886F8F622F63D7E14D61B87A9177AF6EFCBA41E95BA35B2D0E330F9CAE832EA3CAA46DFBA1CB2D88D96B34F5DE2C12255AF89D0BC7FA9E5AAF1FC0A84CC3B6E9BDF25652A44F0DB30C4CEBE9298373CF54E73DA942D060F112B2F525364A3ACB0D2D3DEE2E7F908202D3E7C8FAEC5CFD7E0E3F506272A405D7486A0A7B2C7D9F3F8FC06222546647AAEB4CCFE00000000000000000000000000000000000000000000000000111E2D37");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BC");
        SecureRandom katRandom = new NISTSecureRandom(Hex.decode("061550234D158C5EC95595FE04EF7A25767F2E24CC2BC479D09D86DC9ABCFDE7056A8C266F9EF97ED08541DBD2E1FFA1"), null);

        kpg.initialize(DilithiumParameterSpec.dilithium2, katRandom);

        KeyPair kp = kpg.generateKeyPair();

        SubjectPublicKeyInfo pubInfo = SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded());

        ASN1BitString pubSeq = pubInfo.getPublicKeyData();
        assertTrue(Arrays.areEqual(pubSeq.getOctets(), pubK));

        PrivateKeyInfo privInfo = PrivateKeyInfo.getInstance(kp.getPrivate().getEncoded());
        ASN1OctetString seq = ASN1OctetString.getInstance(privInfo.parsePrivateKey());

        assertTrue(Arrays.areEqual(seq.getOctets(), privK));

        Signature sig = Signature.getInstance("Dilithium", "BC");

        sig.initSign(kp.getPrivate());

        sig.update(msg, 0, msg.length);

        byte[] genS = sig.sign();

        assertTrue(Arrays.areEqual(s, genS));

        sig = Signature.getInstance("Dilithium", "BC");

        sig.initVerify(kp.getPublic());

        sig.update(msg, 0, msg.length);

        assertTrue(sig.verify(s));

        // check randomisation

        sig.initSign(kp.getPrivate(), new SecureRandom());

        sig.update(msg, 0, msg.length);

        genS = sig.sign();

        assertFalse(Arrays.areEqual(s, genS));

        sig = Signature.getInstance("Dilithium", "BC");

        sig.initVerify(kp.getPublic());

        sig.update(msg, 0, msg.length);

        assertTrue(sig.verify(s));
    }

    private static class RiggedRandom
            extends SecureRandom
    {
        public void nextBytes(byte[] bytes)
        {
            for (int i = 0; i != bytes.length; i++)
            {
                bytes[i] = (byte)(i & 0xff);
            }
        }
    }
}
