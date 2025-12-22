import { useState, useMemo } from 'react';

const useInventoryFilter = (items = []) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchField, setSearchField] = useState('all');
    const [visibleColumns, setVisibleColumns] = useState({
        sku: true, name: true, price: true, type: true, stock: true,
        author: false, isbn: false, brand: false, hardness: false
    });
    const [isColumnSelectorOpen, setIsColumnSelectorOpen] = useState(false);

    const toggleColumn = (col) => {
        setVisibleColumns(prev => ({ ...prev, [col]: !prev[col] }));
    };

    const filteredItems = useMemo(() => {
        return items.filter(item => {
            // Support both direct item list (InventoryManager) and stockLevel objects (POS)
            // POS items are wrapped in { product: ... }, InventoryManager items are direct products
            const product = item.product || item;

            const term = searchQuery.toLowerCase();
            if (!term) return true;

            if (searchField === 'all') {
                return product.name.toLowerCase().includes(term) || product.sku.toLowerCase().includes(term);
            } else if (searchField === 'name') {
                return product.name.toLowerCase().includes(term);
            } else if (searchField === 'sku') {
                return product.sku.toLowerCase().includes(term);
            } else {
                // Check attributes
                return product.attributes?.[searchField]?.toLowerCase()?.includes(term);
            }
        });
    }, [items, searchQuery, searchField]);

    return {
        searchQuery, setSearchQuery,
        searchField, setSearchField,
        visibleColumns, toggleColumn,
        isColumnSelectorOpen, setIsColumnSelectorOpen,
        filteredItems
    };
};

export default useInventoryFilter;
