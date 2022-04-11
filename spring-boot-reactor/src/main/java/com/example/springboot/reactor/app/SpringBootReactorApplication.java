package com.example.springboot.reactor.app;

import java.nio.channels.InterruptedByTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.springboot.reactor.app.models.Comentarios;
import com.example.springboot.reactor.app.models.Usuario;
import com.example.springboot.reactor.app.models.UsuarioComentarios;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ejemploContraPresion();
	}
	
	public void ejemploContraPresion() {
		Flux.range(1, 10)
		.log()
		//.limitRate(5) //OTRA FORMA DE HACER ENVIO POR LOTES
		.subscribe(new Subscriber<Integer>() {
			
			private Subscription s;
			
			private Integer limite = 2;
			private Integer consumido = 0;

			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(limite);				
			}

			@Override
			public void onNext(Integer t) {
				log.info(t.toString());
				consumido++;
				if(consumido == limite) {
					consumido = 0;
					s.request(limite);
				}
			}

			@Override
			public void onError(Throwable t) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	public void ejemploIntervalDesdeCreate() {
		Flux.create(emitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {

				private Integer contador = 0;

				@Override
				public void run() {
					emitter.next(++contador);
					if (contador == 10) {
						timer.cancel();
						emitter.complete();
					}

					if (contador == 5) {
						timer.cancel();
						emitter.error(new InterruptedException("Llegó al 5, hay un error en el Flux!"));
					}
				}
			}, 1000, 1000);
		})
				// .doOnNext(next -> log.info(next.toString()))
				// .doOnComplete(() -> log.info("Se ha lleago al final del método!"))
				.subscribe(next -> log.info(next.toString()), error -> log.info(error.getMessage()),
						() -> log.info("Se ha lleago al final del método!"));
	}

	public void ejemploIntervalInfinito() throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(1);

		Flux.interval(Duration.ofSeconds(1)).doOnTerminate(latch::countDown)// .doOnTerminate(() -> latch.countDown())
				.flatMap(i -> {
					if (i >= 5) {
						return Flux.error(new InterruptedException("Solo hasta 5."));
					}
					return Flux.just(i);
				}).map(i -> "Hola " + i)
				// .doOnNext(s -> log.info(s)) SE COMENTA YA QUE SE IMPRIMIRA 2 VECES
				.retry(2) // VUELVE A EJECUTAR TODO LA N CANTIDAD DE VECES
				.subscribe(s -> log.info(s), e -> log.error(e.getMessage()));

		latch.await();

	}

	public void ejemploDelayElements() throws InterruptedException {
		Flux<Integer> rango = Flux.range(1, 12).delayElements(Duration.ofSeconds(1))
				.doOnNext(i -> log.info(i.toString()));

		rango.blockLast(); // BLOQUEA HASTA EL ULTIMO ELEMENTO.
		// rango.subscribe();

		Thread.sleep(12);
	}

	public void ejemploInterval() {
		Flux<Integer> rango = Flux.range(1, 12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));

		rango.zipWith(retraso, (ra, re) -> ra).doOnNext(i -> log.info(i.toString())).blockLast();
		// .subscribe();
	}

	public void ejemploZipWithRangos() {
		Flux<Integer> rangos = Flux.range(0, 4); // SE DECLARA ANTES
		Flux.just(1, 2, 3, 4).map(i -> (i * 2))
				.zipWith(rangos, (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
				.subscribe(texto -> log.info(texto));
	}

	public void ejemploUsuarioComentariosZipWithForma2() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juanito", "Mengano"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola David, qué tal?");
			comentarios.addComentario("Todo bien, estoy emprendiendo!");
			comentarios.addComentario("Qué bien y yo aprendiendo programación reactiva.");
			return comentarios;
		});

		Mono<UsuarioComentarios> usuarioConComentarios = usuarioMono.zipWith(comentariosUsuarioMono) // TIPO DE DATO
																										// TUPLA QUE
																										// CONTIENE
																										// USUARIO Y
																										// COMENTARIOS
				.map(tuple -> {
					Usuario u = tuple.getT1();
					Comentarios c = tuple.getT2();
					return new UsuarioComentarios(u, c);
				});
		usuarioConComentarios.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploUsuarioComentariosZipWith() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juanito", "Mengano"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola David, qué tal?");
			comentarios.addComentario("Todo bien, estoy emprendiendo!");
			comentarios.addComentario("Qué bien y yo aprendiendo programación reactiva.");
			return comentarios;
		});

		Mono<UsuarioComentarios> usuarioConComentarios = usuarioMono.zipWith(comentariosUsuarioMono,
				(u, c) -> new UsuarioComentarios(u, c));
		usuarioConComentarios.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploUsuarioComentariosFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juanito", "Mengano"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola David, qué tal?");
			comentarios.addComentario("Todo bien, estoy emprendiendo!");
			comentarios.addComentario("Qué bien y yo aprendiendo programación reactiva.");
			return comentarios;
		});

		usuarioMono.flatMap(u -> comentariosUsuarioMono.map(c -> new UsuarioComentarios(u, c)))
				.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploCollectList() throws Exception {

		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Jaison", "Vargas"));
		usuariosList.add(new Usuario("Daenerys", "Targaryen"));
		usuariosList.add(new Usuario("Enrique", "Juárez"));
		usuariosList.add(new Usuario("Enrique", "Corto"));
		usuariosList.add(new Usuario("Francesco", "Vargas"));
		usuariosList.add(new Usuario("Morgana", "Tennenbaum"));

		Flux.fromIterable(usuariosList).collectList().subscribe(lista -> {
			lista.forEach(item -> log.info(item.toString()));
		});
	}

	public void ejemploToString() throws Exception {

		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Jaison", "Vargas"));
		usuariosList.add(new Usuario("Daenerys", "Targaryen"));
		usuariosList.add(new Usuario("Enrique", "Juárez"));
		usuariosList.add(new Usuario("Enrique", "Corto"));
		usuariosList.add(new Usuario("Francesco", "Vargas"));
		usuariosList.add(new Usuario("Morgana", "Tennenbaum"));

		Flux.fromIterable(usuariosList)
				.map(usuario -> usuario.getNombre().toUpperCase().concat(" ")
						.concat(usuario.getApellido().toUpperCase()))
				// USO DE FLATMAP
				.flatMap(nombre -> {
					// if(nombre.equalsIgnoreCase("morgana")) {
					if (nombre.contains("morgana".toUpperCase())) {
						return Mono.just(nombre);
					} else {
						return Mono.empty();
					}
				}).map(nombre -> {
					return nombre.toLowerCase();
				}).subscribe(usuario -> log.info(usuario.toString()));
	}

	public void ejemploFlatMap() throws Exception {

		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Jaison Vargas");
		usuariosList.add("Daenerys Targaryen");
		usuariosList.add("Enrique Juárez");
		usuariosList.add("Enrique Corto");
		usuariosList.add("Francesco Vargas");
		usuariosList.add("Morgana Tennenbaum");

		Flux.fromIterable(usuariosList)
				.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				// USO DE FILTER
				// .filter(usuario -> usuario.getNombre().toLowerCase().equals("enrique")) //
				// equalsignorecase
				// USO DE FLATMAP
				.flatMap(usuario -> {
					if (usuario.getNombre().equalsIgnoreCase("morgana")) {
						return Mono.just(usuario);
					} else {
						return Mono.empty();
					}
				}).map(usuario -> { // SE CAMBIA VARIABLE A usuario
					// PRIMERO OBTENER EL NOMBRE DEL USUARIO
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					// LUEGO SE RETORNA EL USUARIO
					return usuario; // SI SALE ERROR ES QUE EL FLUX DEBE CAMBIARSE DE TIPO, YA NO ES STRING
				}).subscribe(usuario -> log.info(usuario.toString()));
	}

	public void ejemploIterable() throws Exception {

		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Jaison Vargas");
		usuariosList.add("Daenerys Targaryen");
		usuariosList.add("Enrique Juárez");
		usuariosList.add("Enrique Corto");
		usuariosList.add("Francesco Vargas");
		usuariosList.add("Morgana Tennenbaum");

		// Flux<String> nombres = Flux.just("Jaison", "Daenerys", "Diego", "Enrique",
		// "Francesco", "Morgana")
		Flux<String> nombres = Flux.fromIterable(usuariosList);// Flux.just("Jaison Vargas", "Daenerys Targaryen",
																// "Enrique Juárez", "Enrique Corto", "Francesco
																// Vargas", "Morgana Tennenbaum");
		// Flux<Usuario> nombres = Flux.just("Jaison Vargas", "Daenerys Targaryen",
		// "Enrique Juárez", "Enrique Corto", "Francesco Vargas", "Morgana Tennenbaum");

		// FORMA PARA CONVERTIR A TIPO USUARIO
		// .map(nombre -> new Usuario(nombre.toUpperCase(), null)
		// SE TRABAJA CON SPLIT
		Flux<Usuario> usuarios = nombres
				.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase())
				/*
				 * .map(nombre -> { return nombre.toUpperCase(); }
				 */)
				// USO DE FILTER
				.filter(usuario -> usuario.getNombre().toLowerCase().equals("enrique")) // equalsignorecase
				.doOnNext(usuario -> {
					// if (e.isEmpty()) {
					if (usuario == null) {
						throw new RuntimeException("Nombre vacío.");
					}
					{
						System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));
					}
//				}).map(nombre -> {
				}).map(usuario -> { // SE CAMBIA VARIABLE A usuario
					// PRIMERO OBTENER EL NOMBRE DEL USUARIO
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					// return nombre.toLowerCase();
					// LUEGO SE RETORNA EL USUARIO
					return usuario; // SI SALE ERROR ES QUE EL FLUX DEBE CAMBIARSE DE TIPO, YA NO ES STRING
				});
		// .doOnNext(element -> System.out.println(element));
		// .doOnNext(System.out::println);

		// SUBSCRIBE CON MANEJO DE ERRORES Y RUNNABLE ONCCOMPLETE
		// nombres.subscribe(e -> log.info(e), error -> log.error(error.getMessage()),
		// new Runnable() {
		// FINALMENTE SE MODIFICA ACA YA QUE EL Log ESTA TRATANDO DE IMPRIMIR EL OBJETO
		// USUARIO
		// SE PUEDE CAMBIAR DE .getNombre POR .toString PARA VER LOS RESULTADOS
		usuarios.subscribe(usuario -> log.info(usuario.toString()), error -> log.error(error.getMessage()),
				new Runnable() {

					@Override
					public void run() {
						log.info("Se ha completado la tarea del flux.");
					}
				});
		// nombres.subscribe(log::info);
	}
}
