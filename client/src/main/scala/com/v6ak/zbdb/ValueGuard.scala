package com.v6ak.zbdb

final class ValueGuard[T] (initialValue: T)(onChange: (T, T) => Unit) {

  private var value = initialValue

  def update(newValue: T): Unit ={
    if(newValue != value){
      val oldValue = value
      value = newValue
      onChange(oldValue, newValue)
    }
  }

}

object ValueGuard{
  @inline def apply[T](initialValue: T)(onChange: (T, T) => Unit): ValueGuard[T] = new ValueGuard(initialValue)(onChange)
}