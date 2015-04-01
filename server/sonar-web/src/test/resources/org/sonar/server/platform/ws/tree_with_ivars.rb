class Tree
  attr_reader :name, :shape, :foliage, :flower
  def initialize(flower)
    @flower = flower
  end
  def to_s
    "#{name.capitalize} is a #{shape} shaped, #{foliage} tree, and blooms #{flower.color} flowers in #{flower.bloomtime}."
  end
  def update
    flower.color = @color
    flower.bloomtime = @bloomtime
  end
end

class Flower
  attr_accessor :color, :bloomtime
  def initialize
  end
end

Tree.new(Flower.new)