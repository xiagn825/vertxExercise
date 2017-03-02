package io.vertx.blog.first;

import io.vertx.core.json.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaguannan on 2017/2/23.
 */
public class Whisky {
  private static final AtomicInteger COUNTER = new AtomicInteger();

  private int id;

  private String name;

  private String origin;

  public Whisky() {
    this.id = COUNTER.getAndIncrement();
  }

  public Whisky(JsonObject result) {
    Method[] methods = this.getClass().getDeclaredMethods();
    Set<String> keySet = result.getMap().keySet();
    for (String key : keySet) {
      String setMethodName = "set" + key;
      for (int j = 0; j < methods.length; j++) {
        System.out.println(methods[j].getName());
        if (methods[j].getName().equalsIgnoreCase(setMethodName)) {
          Object value = result.getValue(key);
          if (value == null) {
            continue;
          }
          try {
            Method setMethod = this.getClass().getMethod(
              methods[j].getName(), value.getClass());
            setMethod.invoke(this, value);
          } catch (NoSuchMethodException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public Whisky(String name, String origin) {
    this.id = COUNTER.getAndIncrement();
    this.name = name;
    this.origin = origin;
  }
  public Whisky(int id, String name, String origin) {
    this.id = id;
    this.name = name;
    this.origin = origin;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public int getId() {
    return id;
  }
}
