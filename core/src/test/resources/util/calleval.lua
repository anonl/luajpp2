test = 0

function x(a, b, c)
    test = a + b + c
    return y()
end

function y()
    return z()
end

function z()
    yield(10)
end
