/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.Decorators;
import io.jooby.FileUpload;
import io.jooby.json.Jackson;

import java.util.concurrent.Executors;

public class HelloApp extends Jooby {

  public static class User {

    public final String id;

    public final String pass;

    public final FileUpload pic;

    public User(String id, String pass, FileUpload pic) {
      this.id = id;
      this.pass = pass;
      this.pic = pic;
    }
  }
  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    mode(ExecutionMode.EVENT_LOOP);


    decorate(next -> ctx -> {
      System.out.println(Thread.currentThread());
      return next.apply(ctx);
    });

    decorate(Decorators.defaultHeaders());

    get("/", ctx -> ctx.sendText(MESSAGE));

//    get("/{foo}", ctx -> ctx.sendText("Hello World!"));

    String text = "Ut viverra erat finibus, interdum felis ac, bibendum nisi. Mauris sit amet augue ac mauris suscipit sagittis. Aenean dui orci, consequat et dictum in, fermentum non lectus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Etiam pulvinar metus nisl, sed consectetur sem iaculis dignissim. Quisque luctus tortor risus, quis sodales turpis rutrum ac. Quisque hendrerit, magna in auctor sollicitudin, lectus lacus aliquam nisi, id dapibus sem mauris vel sem. Donec eu elit congue, tincidunt urna fringilla, euismod metus. Nullam venenatis blandit gravida. Aenean a augue sed orci pretium maximus ac viverra elit. Praesent id pellentesque orci. Nam finibus condimentum mi at iaculis.\n"
        + "Vivamus eget nunc commodo, lacinia magna eget, suscipit arcu. Proin id ullamcorper felis. Suspendisse a hendrerit velit. Sed ullamcorper pulvinar ipsum, eu ultrices nisl fermentum at. Fusce at erat quis ante rutrum porta euismod auctor lorem. Maecenas scelerisque massa id augue luctus lacinia. Praesent tempor scelerisque feugiat. Pellentesque vestibulum fermentum convallis. Donec molestie pretium feugiat. Quisque at tincidunt dolor. Maecenas rhoncus turpis a dui lobortis, sed suscipit felis blandit. Praesent molestie justo lorem, eget semper nulla feugiat tristique.\n"
        + "Curabitur et dui at justo blandit feugiat vel et orci. Vestibulum quis ipsum et mi laoreet tristique. Phasellus nec fermentum tellus. Suspendisse luctus nisl consectetur turpis aliquet, a euismod nibh dignissim. Donec dolor quam, vestibulum vitae porta at, aliquam quis elit. Aenean a neque ut justo hendrerit porta et quis quam. Sed sem lacus, scelerisque nec ultricies ut, dapibus ut justo. Donec sed maximus erat, quis semper purus. Vivamus rhoncus faucibus sem, accumsan venenatis est consectetur sit amet. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam bibendum ac purus ut suscipit. Integer nec vehicula augue, at dapibus lorem. Vivamus eros risus, mattis id dictum nec, porttitor ac libero.\n"
        + "Praesent dictum eleifend nulla, ac venenatis metus efficitur cursus. Phasellus id felis in nibh elementum laoreet a ac turpis. Phasellus finibus sagittis gravida. Duis mattis orci ultrices quam fermentum eleifend. Proin ut diam tempus, porta ante rutrum, bibendum metus. Nam commodo, ante sodales elementum euismod, urna urna volutpat tortor, eget condimentum ex nisl quis neque. Etiam rhoncus velit ut est imperdiet tempus. Nullam dapibus dapibus molestie. Duis lacinia ex et leo dictum, aliquet bibendum orci tincidunt. Maecenas vitae mauris maximus, condimentum mauris id, cursus sem. Etiam vel tellus sit amet magna malesuada egestas.\n"
        + "Quisque enim nisl, rutrum sollicitudin dolor in, venenatis sodales dolor. In et ipsum sed arcu ornare ultrices. Nam ultricies sem non nunc ultricies, id malesuada libero dapibus. Ut mollis, risus sit amet vestibulum eleifend, arcu neque luctus odio, in sollicitudin lorem lectus nec sapien. Phasellus a dolor felis. Integer sagittis risus augue, ac suscipit nulla facilisis vel. Nullam vel metus velit. Donec ut justo eu nunc luctus consequat ut eget mauris. Phasellus ullamcorper metus non ex pellentesque, tempus placerat massa interdum. Etiam quis nunc eget leo aliquet interdum. Praesent malesuada justo ut urna bibendum eleifend. Integer non venenatis lacus. Mauris sit amet egestas turpis.\n"
        + "Etiam non bibendum est. Aenean vel consequat sem. Aenean dictum diam feugiat nulla pulvinar euismod. Etiam eu purus ultricies, tincidunt nisi sed, porttitor arcu. Aenean a pellentesque elit. Quisque ultrices, eros quis hendrerit sodales, erat nisl viverra metus, fringilla hendrerit ante massa sed justo. Ut hendrerit odio vel leo faucibus, nec rhoncus magna volutpat.\n"
        + "Integer nec justo dictum, sodales libero eu, congue erat. Maecenas vel orci libero. Maecenas vitae quam sit amet elit sagittis vestibulum non eu diam. Phasellus eu pellentesque lacus. Donec eget neque et ante scelerisque ultricies. Donec iaculis scelerisque orci nec maximus. Ut id mi sit amet sapien consectetur vestibulum nec sollicitudin elit. Curabitur commodo egestas augue tincidunt vulputate. Etiam semper facilisis turpis a tempus. Vestibulum erat elit, interdum et pretium vitae, congue nec magna. Proin tempus accumsan magna eget ultricies. Maecenas ut suscipit diam, ac rutrum nisi. Praesent vestibulum commodo arcu, in pellentesque libero pellentesque vitae. Sed at libero dignissim, suscipit leo vitae, auctor ligula.\n"
        + "Etiam faucibus accumsan justo eget malesuada. Aenean ac diam porttitor, condimentum odio quis, volutpat leo. Maecenas in dictum elit, in dapibus ante. Etiam molestie, lorem ullamcorper ultrices dignissim, felis velit pretium arcu, vel elementum nisl enim quis diam. Sed finibus vitae dolor sed hendrerit. Donec diam magna, cursus ut turpis eget, mattis vehicula lorem. Fusce a euismod risus, et vestibulum lacus. Ut convallis nibh et pharetra consectetur.\n"
        + "Phasellus mattis malesuada sem ut sodales. Duis ac dui facilisis, accumsan ex a, laoreet orci. Morbi eu quam commodo, ornare mi ut, consectetur ligula. Aenean volutpat venenatis lacus, in porta ligula porttitor varius. Nam id maximus tellus, eget iaculis diam. Pellentesque imperdiet, purus nec sollicitudin accumsan, velit lacus venenatis mauris, auctor porttitor dui lacus ut justo. Curabitur a ligula viverra, placerat ex a, posuere eros. Donec eu nisl a mi varius mattis non ut mi.\n"
        + "Proin et mauris ultrices, lacinia leo at, tristique neque. Cras et odio ac turpis efficitur condimentum sed sed quam. Aenean sed nibh quis mi hendrerit dapibus. Ut non metus ut enim consectetur viverra eu ac orci. Aenean porttitor mattis erat, quis lobortis eros mollis sollicitudin. In elementum orci eu tristique congue. Suspendisse in nulla enim. Phasellus hendrerit quam ut elit molestie, sit amet dapibus nisi cursus. Quisque suscipit fringilla consequat. Integer eu posuere velit, ut blandit tortor. Vivamus aliquam ornare nisi. Sed a venenatis tortor, ut accumsan turpis. Mauris pretium luctus nisi, nec viverra lacus. Maecenas vulputate ex ante, ac pellentesque urna euismod at. Cras ut felis porta, vestibulum ligula et, volutpat neque.\n"
        + "Fusce lobortis lacus magna, nec euismod urna dictum id. Maecenas scelerisque justo vel magna venenatis blandit. Vivamus feugiat lorem eu lorem viverra hendrerit. Nulla in nisl a dolor sodales cursus quis sit amet nisl. Praesent rutrum neque id lacus pharetra gravida. Nunc finibus, lectus sed semper dignissim, urna tellus mattis sapien, nec tempus orci massa eget massa. Sed vestibulum suscipit elementum. Pellentesque faucibus mauris eget malesuada molestie. Sed volutpat quam eleifend dolor ultrices, sit amet tempus ante egestas. Nulla varius libero sit amet mi vehicula faucibus.\n"
        + "Morbi rutrum finibus bibendum. Duis euismod metus in tellus pulvinar, gravida vehicula turpis pulvinar. Duis at ante et nulla aliquet lacinia. Ut sed nulla purus. Donec volutpat non leo sollicitudin facilisis. Maecenas luctus condimentum mi, id bibendum ipsum pellentesque id. Integer facilisis elementum sodales. Mauris tincidunt ornare bibendum. Morbi varius dui non viverra pretium. Pellentesque a gravida sapien. Integer id eleifend turpis.\n"
        + "Etiam felis ante, blandit at libero eu, sagittis molestie leo. In hac habitasse platea dictumst. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec ultrices a augue ac faucibus. Sed luctus bibendum augue, nec ultrices justo semper et. Aenean ullamcorper, turpis eu tempus ultricies, magna augue vestibulum leo, id scelerisque enim urna id nibh. Mauris lacinia posuere vehicula.\n"
        + "Duis tempor magna facilisis nunc commodo cursus. Maecenas nibh quam, vehicula a sapien a, congue tincidunt dui. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Praesent ultrices consequat lectus non pharetra. Fusce justo lorem, pharetra ut leo eu, elementum tincidunt lectus. Cras pretium dignissim arcu at tempor. In blandit molestie mi id fermentum. Curabitur id varius risus. Sed tellus eros, maximus sit amet volutpat quis, cursus quis eros. In vulputate, orci a consequat viverra, velit ex iaculis erat, vitae fermentum enim nunc id odio. Donec faucibus lorem non massa vehicula efficitur. Suspendisse potenti. Donec elit lectus, pellentesque vitae gravida id, pretium vestibulum est. Mauris porttitor ipsum quis nisi tincidunt pretium.\n"
        + "Nulla iaculis dignissim risus ut sagittis. Suspendisse justo tortor, mattis cursus consequat sed, pulvinar nec diam. Maecenas luctus maximus blandit. Nunc vel augue quis urna rhoncus tempor in vel dolor. Proin hendrerit, velit non elementum consequat, odio mi molestie nisi, vitae bibendum ex urna quis purus. Vivamus dui dolor, venenatis ac nisl eget, sollicitudin egestas nunc. Maecenas tristique nisl quis erat tincidunt, eget semper nibh pharetra. Integer id efficitur purus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Proin ut porttitor sem, aliquet sagittis tellus. Aenean congue ornare eros, eget dignissim dolor malesuada at.\n"
        + "Aenean elementum lectus a convallis placerat. In placerat, nisl id pellentesque vestibulum, nisl leo consequat lorem, viverra dignissim risus magna sit amet nunc. Sed vitae velit et turpis vehicula finibus. Ut quis velit ac libero molestie eleifend. Aliquam erat volutpat. Phasellus sollicitudin justo ac ligula semper posuere. Fusce ullamcorper nisl ut placerat facilisis. Nunc lectus mauris, malesuada ac pretium ut, mollis ac velit.\n"
        + "Ut viverra erat finibus, interdum felis ac, bibendum nisi. Mauris sit amet augue ac mauris suscipit sagittis. Aenean dui orci, consequat et dictum in, fermentum non lectus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Etiam pulvinar metus nisl, sed consectetur sem iaculis dignissim. Quisque luctus tortor risus, quis sodales turpis rutrum ac. Quisque hendrerit, magna in auctor sollicitudin, lectus lacus aliquam nisi, id dapibus sem mauris vel sem. Donec eu elit congue, tincidunt urna fringilla, euismod metus. Nullam venenatis blandit gravida. Aenean a augue sed orci pretium maximus ac viverra elit. Praesent id pellentesque orci. Nam finibus condimentum mi at iaculis.\n"
        + "Vivamus eget nunc commodo, lacinia magna eget, suscipit arcu. Proin id ullamcorper felis. Suspendisse a hendrerit velit. Sed ullamcorper pulvinar ipsum, eu ultrices nisl fermentum at. Fusce at erat quis ante rutrum porta euismod auctor lorem. Maecenas scelerisque massa id augue luctus lacinia. Praesent tempor scelerisque feugiat. Pellentesque vestibulum fermentum convallis. Donec molestie pretium feugiat. Quisque at tincidunt dolor. Maecenas rhoncus turpis a dui lobortis, sed suscipit felis blandit. Praesent molestie justo lorem, eget semper nulla feugiat tristique.\n"
        + "Curabitur et dui at justo blandit feugiat vel et orci. Vestibulum quis ipsum et mi laoreet tristique. Phasellus nec fermentum tellus. Suspendisse luctus nisl consectetur turpis aliquet, a euismod nibh dignissim. Donec dolor quam, vestibulum vitae porta at, aliquam quis elit. Aenean a neque ut justo hendrerit porta et quis quam. Sed sem lacus, scelerisque nec ultricies ut, dapibus ut justo. Donec sed maximus erat, quis semper purus. Vivamus rhoncus faucibus sem, accumsan venenatis est consectetur sit amet. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam bibendum ac purus ut suscipit. Integer nec vehicula augue, at dapibus lorem. Vivamus eros risus, mattis id dictum nec, porttitor ac libero.\n"
        + "Praesent dictum eleifend nulla, ac venenatis metus efficitur cursus. Phasellus id felis in nibh elementum laoreet a ac turpis. Phasellus finibus sagittis gravida. Duis mattis orci ultrices quam fermentum eleifend. Proin ut diam tempus, porta ante rutrum, bibendum metus. Nam commodo, ante sodales elementum euismod, urna urna volutpat tortor, eget condimentum ex nisl quis neque. Etiam rhoncus velit ut est imperdiet tempus. Nullam dapibus dapibus molestie. Duis lacinia ex et leo dictum, aliquet bibendum orci tincidunt. Maecenas vitae mauris maximus, condimentum mauris id, cursus sem. Etiam vel tellus sit amet magna malesuada egestas.\n"
        + "Quisque enim nisl, rutrum sollicitudin dolor in, venenatis sodales dolor. In et ipsum sed arcu ornare ultrices. Nam ultricies sem non nunc ultricies, id malesuada libero dapibus. Ut mollis, risus sit amet vestibulum eleifend, arcu neque luctus odio, in sollicitudin lorem lectus nec sapien. Phasellus a dolor felis. Integer sagittis risus augue, ac suscipit nulla facilisis vel. Nullam vel metus velit. Donec ut justo eu nunc luctus consequat ut eget mauris. Phasellus ullamcorper metus non ex pellentesque, tempus placerat massa interdum. Etiam quis nunc eget leo aliquet interdum. Praesent malesuada justo ut urna bibendum eleifend. Integer non venenatis lacus. Mauris sit amet egestas turpis.\n"
        + "Etiam non bibendum est. Aenean vel consequat sem. Aenean dictum diam feugiat nulla pulvinar euismod. Etiam eu purus ultricies, tincidunt nisi sed, porttitor arcu. Aenean a pellentesque elit. Quisque ultrices, eros quis hendrerit sodales, erat nisl viverra metus, fringilla hendrerit ante massa sed justo. Ut hendrerit odio vel leo faucibus, nec rhoncus magna volutpat.\n"
        + "Integer nec justo dictum, sodales libero eu, congue erat. Maecenas vel orci libero. Maecenas vitae quam sit amet elit sagittis vestibulum non eu diam. Phasellus eu pellentesque lacus. Donec eget neque et ante scelerisque ultricies. Donec iaculis scelerisque orci nec maximus. Ut id mi sit amet sapien consectetur vestibulum nec sollicitudin elit. Curabitur commodo egestas augue tincidunt vulputate. Etiam semper facilisis turpis a tempus. Vestibulum erat elit, interdum et pretium vitae, congue nec magna. Proin tempus accumsan magna eget ultricies. Maecenas ut suscipit diam, ac rutrum nisi. Praesent vestibulum commodo arcu, in pellentesque libero pellentesque vitae. Sed at libero dignissim, suscipit leo vitae, auctor ligula.\n"
        + "Etiam faucibus accumsan justo eget malesuada. Aenean ac diam porttitor, condimentum odio quis, volutpat leo. Maecenas in dictum elit, in dapibus ante. Etiam molestie, lorem ullamcorper ultrices dignissim, felis velit pretium arcu, vel elementum nisl enim quis diam. Sed finibus vitae dolor sed hendrerit. Donec diam magna, cursus ut turpis eget, mattis vehicula lorem. Fusce a euismod risus, et vestibulum lacus. Ut convallis nibh et pharetra consectetur.\n"
        + "Phasellus mattis malesuada sem ut sodales. Duis ac dui facilisis, accumsan ex a, laoreet orci. Morbi eu quam commodo, ornare mi ut, consectetur ligula. Aenean volutpat venenatis lacus, in porta ligula porttitor varius. Nam id maximus tellus, eget iaculis diam. Pellentesque imperdiet, purus nec sollicitudin accumsan, velit lacus venenatis mauris, auctor porttitor dui lacus ut justo. Curabitur a ligula viverra, placerat ex a, posuere eros. Donec eu nisl a mi varius mattis non ut mi.\n"
        + "Proin et mauris ultrices, lacinia leo at, tristique neque. Cras et odio ac turpis efficitur condimentum sed sed quam. Aenean sed nibh quis mi hendrerit dapibus. Ut non metus ut enim consectetur viverra eu ac orci. Aenean porttitor mattis erat, quis lobortis eros mollis sollicitudin. In elementum orci eu tristique congue. Suspendisse in nulla enim. Phasellus hendrerit quam ut elit molestie, sit amet dapibus nisi cursus. Quisque suscipit fringilla consequat. Integer eu posuere velit, ut blandit tortor. Vivamus aliquam ornare nisi. Sed a venenatis tortor, ut accumsan turpis. Mauris pretium luctus nisi, nec viverra lacus. Maecenas vulputate ex ante, ac pellentesque urna euismod at. Cras ut felis porta, vestibulum ligula et, volutpat neque.\n"
        + "Fusce lobortis lacus magna, nec euismod urna dictum id. Maecenas scelerisque justo vel magna venenatis blandit. Vivamus feugiat lorem eu lorem viverra hendrerit. Nulla in nisl a dolor sodales cursus quis sit amet nisl. Praesent rutrum neque id lacus pharetra gravida. Nunc finibus, lectus sed semper dignissim, urna tellus mattis sapien, nec tempus orci massa eget massa. Sed vestibulum suscipit elementum. Pellentesque faucibus mauris eget malesuada molestie. Sed volutpat quam eleifend dolor ultrices, sit amet tempus ante egestas. Nulla varius libero sit amet mi vehicula faucibus.\n"
        + "Morbi rutrum finibus bibendum. Duis euismod metus in tellus pulvinar, gravida vehicula turpis pulvinar. Duis at ante et nulla aliquet lacinia. Ut sed nulla purus. Donec volutpat non leo sollicitudin facilisis. Maecenas luctus condimentum mi, id bibendum ipsum pellentesque id. Integer facilisis elementum sodales. Mauris tincidunt ornare bibendum. Morbi varius dui non viverra pretium. Pellentesque a gravida sapien. Integer id eleifend turpis.\n"
        + "Etiam felis ante, blandit at libero eu, sagittis molestie leo. In hac habitasse platea dictumst. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec ultrices a augue ac faucibus. Sed luctus bibendum augue, nec ultrices justo semper et. Aenean ullamcorper, turpis eu tempus ultricies, magna augue vestibulum leo, id scelerisque enim urna id nibh. Mauris lacinia posuere vehicula.\n"
        + "Duis tempor magna facilisis nunc commodo cursus. Maecenas nibh quam, vehicula a sapien a, congue tincidunt dui. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Praesent ultrices consequat lectus non pharetra. Fusce justo lorem, pharetra ut leo eu, elementum tincidunt lectus. Cras pretium dignissim arcu at tempor. In blandit molestie mi id fermentum. Curabitur id varius risus. Sed tellus eros, maximus sit amet volutpat quis, cursus quis eros. In vulputate, orci a consequat viverra, velit ex iaculis erat, vitae fermentum enim nunc id odio. Donec faucibus lorem non massa vehicula efficitur. Suspendisse potenti. Donec elit lectus, pellentesque vitae gravida id, pretium vestibulum est. Mauris porttitor ipsum quis nisi tincidunt pretium.\n"
        + "Nulla iaculis dignissim risus ut sagittis. Suspendisse justo tortor, mattis cursus consequat sed, pulvinar nec diam. Maecenas luctus maximus blandit. Nunc vel augue quis urna rhoncus tempor in vel dolor. Proin hendrerit, velit non elementum consequat, odio mi molestie nisi, vitae bibendum ex urna quis purus. Vivamus dui dolor, venenatis ac nisl eget, sollicitudin egestas nunc. Maecenas tristique nisl quis erat tincidunt, eget semper nibh pharetra. Integer id efficitur purus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Proin ut porttitor sem, aliquet sagittis tellus. Aenean congue ornare eros, eget dignissim dolor malesuada at.\n"
        + "Aenean elementum lectus a convallis placerat. In placerat, nisl id pellentesque vestibulum, nisl leo consequat lorem, viverra dignissim risus magna sit amet nunc. Sed vitae velit et turpis vehicula finibus. Ut quis velit ac libero molestie eleifend. Aliquam erat volutpat. Phasellus sollicitudin justo ac ligula semper posuere. Fusce ullamcorper nisl ut placerat facilisis. Nunc lectus mauris, malesuada ac pretium ut, mollis ac velit.\n";
    get("/writer", ctx -> {
      return ctx.writer(writer -> {
        writer.append("Foo bar");
      });
    });

    post("/user", ctx -> {
      User user = ctx.multipart(User.class);
      return user.pic.toString();
    });

    post("/docx", ctx -> {
      byte[] bytes = ctx.body().bytes();
      return bytes.length;
    });

    install(new Jackson());
    get("/json", ctx -> new Message("Hello World!"));

  }

  public static void main(String[] args) {
    run(HelloApp::new, args);
  }
}
